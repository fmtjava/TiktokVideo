package com.fmt.nav_annotation.compiler

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider


/**
 * KSP 处理器提供者
 * 用于创建 SymbolProcessor 实例
 */
class NavProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return NavProcessor(codeGenerator = environment.codeGenerator, logger = environment.logger)
    }
}