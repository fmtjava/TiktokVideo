package com.fmt.nav_annotation.compiler

import com.fmt.nav_annotation.NavData
import com.fmt.nav_annotation.NavDestination
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * 导航注解处理器（KSP 版本）
 * 编译期注解处理：在 .kt 文件编译前，扫描代码中指定的注解（@NavDestination），
 * 并根据注解信息执行逻辑。
 */
class NavProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    // 导航页面集合
    private val navDataList = mutableListOf<NavData>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("NavProcessor: enter process....")

        // 1. 获取所有被 @NavDestination 标记的类
        val symbols = resolver
            .getSymbolsWithAnnotation(NavDestination::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        // 2. 处理每个被标记的类
        val ret = symbols.filter { !it.validate() }.toList()

        symbols
            .filter { it.validate() }
            .forEach { classDeclaration ->
                processClass(classDeclaration)
            }

        // 3. 如果收集到了数据，生成代码
        if (navDataList.isNotEmpty()) {
            generateNavRegistry(resolver)
            navDataList.clear() // 清空，防止重复生成
        }

        return ret
    }

    /**
     * 处理单个被 @NavDestination 标记的类
     */
    private fun processClass(classDeclaration: KSClassDeclaration) {
        // 获取类的全限定名
        val className = classDeclaration.qualifiedName?.asString()
            ?: run {
                logger.error("Class name is null", classDeclaration)
                return
            }

        // 获取 @NavDestination 注解
        val annotation = classDeclaration.annotations
            .find { it.shortName.asString() == "NavDestination" }
            ?: run {
                logger.error("Annotation not found", classDeclaration)
                return
            }

        // 解析注解参数
        val route = annotation.arguments
            .find { it.name?.asString() == "route" }
            ?.value as? String
            ?: run {
                logger.error("Route parameter not found", classDeclaration)
                return
            }

        val asStarter = annotation.arguments
            .find { it.name?.asString() == "asStarter" }
            ?.value as? Boolean
            ?: false

        val typeArg = annotation.arguments.find { it.name?.asString() == "type" }?.value
        val navType = when {
            typeArg == null -> NavDestination.NavType.None
            else -> {
                val typeStr = typeArg.toString()
                when {
                    typeStr.contains(
                        "Fragment",
                        ignoreCase = true
                    ) -> NavDestination.NavType.Fragment

                    typeStr.contains(
                        "Activity",
                        ignoreCase = true
                    ) -> NavDestination.NavType.Activity

                    typeStr.contains("Dialog", ignoreCase = true) -> NavDestination.NavType.Dialog
                    typeStr.contains("None", ignoreCase = true) -> NavDestination.NavType.None
                    else -> NavDestination.NavType.None
                }
            }
        }

        // 构建 NavData
        val navData = NavData(route, className, asStarter, navType)
        navDataList.add(navData)

        logger.info(
            "NavProcessor: route=$route, asStarter=$asStarter, " +
                    "navType=$navType, className=$className"
        )
    }

    /**
     *  利用 kotlinPoet 生成 NavRegistry.kt 文件，存放在 nav-annotation 模块下，用于记录项目中所有的路由节点数据
     */
    private fun generateNavRegistry(resolver: Resolver) {
        // 1. 生成成员变量 val navList:ArrayList<NavData>
        val navData = ClassName(NAV_RUNTIME_PKG_NAME, NAV_RUNTIME_NAV_DATA_CLASS_NAME)
        val arrayList =
            ClassName(KOTLIN_COLLECTIONS_PKG_NAME, KOTLIN_COLLECTIONS_ARRAYLIST_CLASS_NAME)
        val list = ClassName(KOTLIN_COLLECTIONS_PKG_NAME, KOTLIN_COLLECTIONS_LIST_CLASS_NAME)
        val listOfNavData = list.parameterizedBy(navData)
        val arrayListOfNavData = arrayList.parameterizedBy(navData)

        // 2. 生成 object Class init { 代码块 }
        val statements = StringBuilder()
        navDataList.forEach {
            statements.append(
                String.format(
                    "navList.add(NavData(\"%s\",\"%s\",%s,%s))",
                    it.route,
                    it.className,
                    it.asStarter,
                    it.type
                )
            )
            statements.append("\n")
        }

        // 3.向 object class 添加成员属性navList并且进行初始化赋值
        val property =
            PropertySpec.builder(NAV_RUNTIME_NAV_LIST, arrayListOfNavData, KModifier.PRIVATE)
                .initializer(CodeBlock.builder().addStatement("ArrayList<NavData>()").build())
                .build()

        // 4.构建 get 方法 - 返回 List<NavData>，支持 listIterator()
        val function = FunSpec.builder("getNavList")
            .returns(listOfNavData)
            .addStatement("return navList")
            .build()

        // 5.构建 object NavRegistry class. 并且填充属性、int{}  get 方法
        val typeSpec = TypeSpec.objectBuilder(NAV_RUNTIME_REGISTRY_CLASS_NAME)
            .addProperty(property)
            .addInitializerBlock(CodeBlock.builder().addStatement(statements.toString()).build())
            .addFunction(function)
            .build()

        // 6.生成文件、添加注释和导包
        val fileSpec = FileSpec.builder(NAV_RUNTIME_PKG_NAME, NAV_RUNTIME_REGISTRY_CLASS_NAME)
            .addFileComment("this file is generated by auto,please do not modify!!!")
            .addType(typeSpec)
            .addImport(NAV_DATA_PKG_NAME, NAV_DATA_CLASS_NAME)
            .addImport(NavDestination.NavType::class.java, "Fragment", "Dialog", "Activity", "None")
            .build()

        // 7. 写入文件（KSP 方式）
        try {
            // 使用 Dependencies(true) 表示依赖于所有输入文件
            // 这样 KSP 在增量编译时不会删除这个文件
            val dependencies = Dependencies(true)
            
            val outputStream = codeGenerator.createNewFile(
                dependencies,
                NAV_RUNTIME_PKG_NAME,
                NAV_RUNTIME_REGISTRY_CLASS_NAME
            )

            // 使用 OutputStreamWriter 写入文件内容
            // FileSpec.writeTo() 接受 Appendable 参数，OutputStreamWriter 实现了 Appendable
            OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                fileSpec.writeTo(writer)
            }

            logger.info("NavProcessor: Generated NavRegistry.kt successfully at package: $NAV_RUNTIME_PKG_NAME")
        } catch (e: Exception) {
            logger.error("NavProcessor: Failed to generate NavRegistry.kt: ${e.message}")
        }
    }

    companion object {
        private const val NAV_RUNTIME_PKG_NAME: String = "com.fmt.tiktokvideo.nav"
        private const val NAV_RUNTIME_REGISTRY_CLASS_NAME: String = "NavRegistry"
        private const val NAV_DATA_PKG_NAME: String = "com.fmt.nav_annotation"
        private const val NAV_DATA_CLASS_NAME: String = "NavData"
        private const val KOTLIN_COLLECTIONS_PKG_NAME: String = "kotlin.collections"
        private const val KOTLIN_COLLECTIONS_LIST_CLASS_NAME: String = "List"
        private const val KOTLIN_COLLECTIONS_ARRAYLIST_CLASS_NAME: String = "ArrayList"
        private const val NAV_RUNTIME_NAV_DATA_CLASS_NAME: String = "NavData"
        private const val NAV_RUNTIME_NAV_LIST: String = "navList"
    }
}
