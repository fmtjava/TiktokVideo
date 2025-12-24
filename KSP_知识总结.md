# KSP (Kotlin Symbol Processing) 知识总结

## 1. 什么是 KSP？

**KSP (Kotlin Symbol Processing)** 是 Google 开发的 Kotlin 符号处理工具，用于在编译时处理 Kotlin 代码。

### 核心特点
- **原生支持 Kotlin**：直接处理 Kotlin AST，不需要生成 Java Stub
- **性能更好**：比 KAPT 快 2-3 倍
- **支持 Kotlin 特性**：suspend、inline、reified 等
- **增量编译**：智能的依赖管理

## 2. 为什么需要 KSP？

### KAPT 的问题
```
Kotlin 源码 → KAPT → Java Stub → APT → 生成代码
```
- 需要生成 Java Stub，编译慢
- 对 Kotlin 特性支持有限
- 增量编译支持不好

### KSP 的优势
```
Kotlin 源码 → KSP → 生成代码
```
- 直接处理 Kotlin AST，无需 Java Stub
- 编译速度更快
- 完整支持 Kotlin 特性

## 3. KSP vs KAPT 对比

| 特性 | KAPT | KSP |
|------|------|-----|
| **处理对象** | Java 元素（Element） | Kotlin 符号（Symbol） |
| **API** | `javax.annotation.processing` | `com.google.devtools.ksp` |
| **注册方式** | `@AutoService` | `SymbolProcessorProvider` + `META-INF/services` |
| **获取注解** | `getAnnotation()` | `annotations.find()` |
| **类型系统** | Java 类型 | Kotlin 类型 |
| **性能** | 较慢（需要生成 Stub） | 快 2-3 倍 |
| **Kotlin 特性** | 支持有限 | 完整支持 |

## 4. KSP 核心概念

### 4.1 Symbol（符号）

KSP 中的符号代表 Kotlin 代码中的各种元素：

```kotlin
// 类符号
KSClassDeclaration

// 函数符号
KSFunctionDeclaration

// 属性符号
KSPropertyDeclaration

// 参数符号
KSValueParameter

// 类型引用
KSTypeReference
```

### 4.2 Resolver（解析器）

`Resolver` 用于查找和解析符号：

```kotlin
// 获取所有被特定注解标记的符号
resolver.getSymbolsWithAnnotation("com.example.MyAnnotation")

// 获取所有文件
resolver.getAllFiles()

// 获取类
resolver.getClassDeclarationByName("com.example.MyClass")

// 获取函数
resolver.getFunctionDeclarations()
```

### 4.3 CodeGenerator（代码生成器）

用于生成新文件：

```kotlin
val outputStream = codeGenerator.createNewFile(
    dependencies,      // 依赖关系
    packageName,      // 包名
    fileName          // 文件名（不含扩展名）
)
```

### 4.4 Dependencies（依赖关系）

管理生成文件的依赖，防止增量编译时被删除：

```kotlin
// 不依赖任何文件（可能被删除）
Dependencies(false)

// 依赖所有输入文件（推荐）
Dependencies(true)

// 依赖特定文件
Dependencies(true, file1, file2, ...)
```

## 5. KSP API 详解

### 5.1 获取注解元素

```kotlin
// 获取所有被 @MyAnnotation 标记的类
val symbols = resolver
    .getSymbolsWithAnnotation("com.example.MyAnnotation")
    .filterIsInstance<KSClassDeclaration>()

// 验证符号有效性
val validSymbols = symbols.filter { it.validate() }
```

### 5.2 读取注解信息

```kotlin
// 获取注解
val annotation = classDeclaration.annotations
    .find { it.shortName.asString() == "MyAnnotation" }

// 读取注解参数
val route = annotation?.arguments
    ?.find { it.name?.asString() == "route" }
    ?.value as? String

val enabled = annotation?.arguments
    ?.find { it.name?.asString() == "enabled" }
    ?.value as? Boolean ?: false
```

### 5.3 处理枚举值

```kotlin
val typeArg = annotation.arguments
    .find { it.name?.asString() == "type" }
    ?.value

// 枚举值在 KSP 中通常以字符串形式返回
val typeStr = typeArg.toString()
val enumValue = when {
    typeStr.contains("Fragment") -> MyEnum.Fragment
    typeStr.contains("Activity") -> MyEnum.Activity
    else -> MyEnum.None
}
```

### 5.4 获取类型信息

```kotlin
// 获取类名
val className = classDeclaration.qualifiedName?.asString()

// 获取包名
val packageName = classDeclaration.packageName.asString()

// 获取父类
val superTypes = classDeclaration.superTypes

// 获取属性
val properties = classDeclaration.getAllProperties()

// 获取函数
val functions = classDeclaration.getAllFunctions()
```

### 5.5 生成文件

```kotlin
// 1. 创建依赖关系
val dependencies = Dependencies(true)

// 2. 创建文件
val outputStream = codeGenerator.createNewFile(
    dependencies,
    "com.example.generated",
    "MyGeneratedClass"
)

// 3. 写入内容（使用 KotlinPoet）
OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
    fileSpec.writeTo(writer)
}
```

## 6. 完整示例

### 6.1 SymbolProcessor 实现

```kotlin
class MyProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 1. 获取所有被标记的符号
        val symbols = resolver
            .getSymbolsWithAnnotation("com.example.MyAnnotation")
            .filterIsInstance<KSClassDeclaration>()

        // 2. 分离有效和无效的符号
        val invalidSymbols = symbols.filter { !it.validate() }.toList()
        val validSymbols = symbols.filter { it.validate() }

        // 3. 处理有效符号
        validSymbols.forEach { classDeclaration ->
            processClass(classDeclaration)
        }

        // 4. 生成代码
        if (dataList.isNotEmpty()) {
            generateCode()
        }

        // 5. 返回无效符号
        return invalidSymbols
    }

    private fun processClass(classDeclaration: KSClassDeclaration) {
        // 处理逻辑
    }

    private fun generateCode() {
        // 生成代码逻辑
    }
}
```

### 6.2 SymbolProcessorProvider 实现

```kotlin
class MyProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return MyProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}
```

### 6.3 注册文件

创建文件：`src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`

内容：
```
com.example.MyProcessorProvider
```

## 7. 最佳实践

### 7.1 依赖关系管理

```kotlin
// ✅ 推荐：依赖所有输入文件
val dependencies = Dependencies(true)

// ❌ 不推荐：不依赖任何文件（可能被删除）
val dependencies = Dependencies(false)
```

### 7.2 符号验证

```kotlin
// ✅ 推荐：验证符号有效性
symbols.filter { it.validate() }.forEach { ... }

// ❌ 不推荐：不验证
symbols.forEach { ... }
```

### 7.3 错误处理

```kotlin
// ✅ 推荐：使用 logger 记录错误
logger.error("Error message", symbol)

// ❌ 不推荐：直接抛出异常
throw Exception("Error")
```

### 7.4 性能优化

```kotlin
// ✅ 推荐：延迟处理
val symbols = resolver.getSymbolsWithAnnotation(...)
    .toList()  // 先转换为 List，避免多次遍历

// ❌ 不推荐：多次遍历
symbols.forEach { ... }
symbols.forEach { ... }
```

## 8. 常见问题

### 8.1 文件被删除

**问题**：生成的文件在增量编译时被删除

**解决**：使用 `Dependencies(true)` 而不是 `Dependencies(false)`

```kotlin
val dependencies = Dependencies(true)  // ✅
// val dependencies = Dependencies(false)  // ❌
```

### 8.2 枚举值处理

**问题**：无法直接获取枚举值

**解决**：通过字符串匹配

```kotlin
val typeStr = annotation.arguments
    .find { it.name?.asString() == "type" }
    ?.value
    ?.toString()

val enumValue = when {
    typeStr?.contains("Fragment") == true -> MyEnum.Fragment
    typeStr?.contains("Activity") == true -> MyEnum.Activity
    else -> MyEnum.None
}
```

### 8.3 可选值处理

**问题**：KSP API 返回很多可选值

**解决**：使用安全调用和默认值

```kotlin
val className = classDeclaration.qualifiedName?.asString()
    ?: run {
        logger.error("Class name is null", classDeclaration)
        return
    }
```

### 8.4 类型转换

**问题**：注解参数需要类型转换

**解决**：使用 `as?` 安全转换

```kotlin
val route = annotation.arguments
    .find { it.name?.asString() == "route" }
    ?.value as? String
    ?: run {
        logger.error("Route not found", classDeclaration)
        return
    }
```

## 9. 迁移指南

### 从 KAPT 迁移到 KSP

1. **修改 build.gradle**
   ```gradle
   // 移除
   id 'kotlin-kapt'
   
   // 添加
   id 'com.google.devtools.ksp' version '1.9.20-1.0.14'
   ```

2. **修改依赖**
   ```gradle
   // 移除
   kapt project(':processor')
   
   // 添加
   ksp project(':processor')
   ```

3. **修改 Processor**
   - 从 `AbstractProcessor` 改为 `SymbolProcessor`
   - 从 `@AutoService` 改为 `SymbolProcessorProvider`
   - 修改注解扫描和读取逻辑

4. **修改文件生成**
   - 从 `Filer` 改为 `CodeGenerator`
   - 从 `File` 改为 `OutputStream`/`Writer`

## 10. 调试技巧

### 10.1 使用 Logger

```kotlin
logger.info("Processing class: ${classDeclaration.qualifiedName}")
logger.warn("Warning message", symbol)
logger.error("Error message", symbol)
```

### 10.2 检查生成的文件

生成的文件位置：
```
app/build/generated/ksp/kotlin/main/包名/文件名.kt
```

### 10.3 查看编译日志

在 Build 输出中查看 KSP 日志，查找：
- `NavProcessor: enter process....`
- `NavProcessor: Generated NavRegistry.kt successfully`
- `NavProcessor: Failed to generate...`

## 11. 总结

### KSP 核心优势
1. **性能更好**：无需生成 Java Stub
2. **支持完整**：支持所有 Kotlin 特性
3. **增量编译**：智能依赖管理
4. **未来趋势**：Google 官方推荐

### 关键要点
1. **符号验证**：始终使用 `validate()` 检查
2. **依赖管理**：使用 `Dependencies(true)` 防止文件被删除
3. **错误处理**：使用 `logger` 记录错误
4. **类型安全**：使用安全调用和类型转换

### 学习资源
- [KSP 官方文档](https://kotlinlang.org/docs/ksp-overview.html)
- [KSP GitHub](https://github.com/google/ksp)
- [KotlinPoet 文档](https://square.github.io/kotlinpoet/)

