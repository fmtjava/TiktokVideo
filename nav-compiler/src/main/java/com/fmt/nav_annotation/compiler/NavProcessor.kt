package com.fmt.nav_annotation.compiler

import com.fmt.nav_annotation.NavData
import com.fmt.nav_annotation.NavDestination
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

/**
 *  导航注解处理器，编译期注解处理：在 .java文件编译为 .class前，扫描代码中指定的注解（@NavDestination），
 *                             并根据注解信息执行逻辑。
 */
@AutoService(Processor::class) // AutoService简化注册
@SupportedSourceVersion(SourceVersion.RELEASE_11) // 等价于 getSupportedSourceVersion()
@SupportedAnnotationTypes("com.fmt.nav_annotation.NavDestination") // 等价于 getSupportedAnnotationTypes()
class NavProcessor : AbstractProcessor() {

    // 导航页面集合
    private val navDataList = mutableListOf<NavData>()
    // 日志输出工具，方便调试
    private lateinit var mMessAger: Messager
    // 文件生成工具
    private lateinit var mFiler: Filer

    /**
     * 初始化处理器
     */
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        mMessAger = processingEnv.messager
        mFiler = processingEnv.filer

        mMessAger.printMessage(Diagnostic.Kind.NOTE, "enter init....")
    }

    /*
     * 遍历注解并处理，roundEnv提供当前轮次的所有注解元素
     */
    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        // 获取 Module 中所有被 @NavDestination 标记的元素
        val elements = roundEnv.getElementsAnnotatedWith(NavDestination::class.java)
        if (elements.isNotEmpty()) {
            // 这里注意：要先 clear()，防止重复添加，因为编译器可能分多轮调用 process()
            navDataList.clear()
            for (element in elements) {
                val typeElement = element as TypeElement
                // 全类名
                val clazzName = typeElement.qualifiedName.toString()
                // 获取类中标记的 @NavDestination 注解
                val annotation = typeElement.getAnnotation(NavDestination::class.java)
                // 获取 @NavDestination 注解中的 type、route、asStarter 等参数
                val route = annotation.route
                val asStarter = annotation.asStarter
                val navType = annotation.type
                // 构建 NavData
                val navData = NavData(route, clazzName, asStarter, navType)
                navDataList.add(navData)
                mMessAger.printMessage(
                    Diagnostic.Kind.NOTE,
                    "route=${route},asStarter=${asStarter},navType=${navType},clazzName=${clazzName}"
                )
            }
            generateNavRegistry()
        }
        return false
    }

    /**
     *  利用 kotlinPoet 生成 NavRegistry.kt 文件，存放在 nav-annotation 模块下，用于记录项目中所有的路由节点数据
     */
    private fun generateNavRegistry() {
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

        // 4.构建 get 方法
        val function = FunSpec.builder("getNavList").returns(listOfNavData)
            .addCode(CodeBlock.builder().addStatement("return navList\n").build()).build()

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

        // 7. 写入文件
        try {
            val resource = mFiler.createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                NAV_RUNTIME_REGISTRY_CLASS_NAME
            )
            val resourcePath = resource.toUri().path
            val appPath = resourcePath.substring(0, resourcePath.indexOf("app") + 4)
            mMessAger.printMessage(Diagnostic.Kind.NOTE, "appPath=${appPath}")
            val javaFilePath = File("${appPath}src/main/java")
            if (javaFilePath.exists()) {
                javaFilePath.mkdirs()
            }
            fileSpec.writeTo(javaFilePath)
        } catch (e: Exception) {
            e.printStackTrace()
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
