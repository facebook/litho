/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.facebook.litho

import com.facebook.litho.common.LithoCompilerConfig
import com.facebook.litho.util.testData
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

abstract class AbstractCompilerTest {
  private val processor = LithoCommandLineProcessor()

  fun compile(
      vararg code: SourceFile,
      options: List<PluginOption> = listOf(LithoCompilerConfig.ENABLED.asOption("true")),
      useK2: Boolean = false
  ): JvmCompilationResult {
    val compilation =
        newCompilation(useK2) {
          sources = code.asList()
          compilerPluginRegistrars = listOf(LithoComponentRegistrar())
          pluginOptions = options
        }
    return compilation.compile()
  }

  fun runTest(
      path: String,
      options: List<PluginOption> = listOf(LithoCompilerConfig.ENABLED.asOption("true")),
      useK2: Boolean = false
  ) {
    val testData = testData(path)
    val compilation =
        newCompilation(useK2) {
          sources = testData.sourceFiles
          compilerPluginRegistrars =
              listOf(LithoComponentRegistrar(), TestComponentRegistrar(testData.processor))
          pluginOptions = options
        }
    val compilationResult = compilation.compile()
    val processorActualResult = testData.processor.getProcessorResult()
    val compilerActualResult = testData.processor.extractCompilerResult(compilationResult)
    assertThat(processorActualResult).isEqualTo(testData.processorExpectedOutput)
    assertThat(compilerActualResult).isEqualTo(testData.compilerExpectedOutput)
  }

  fun LithoCompilerConfig<*>.asOption(value: String): PluginOption {
    return PluginOption(processor.pluginId, cliOption.optionName, value)
  }

  private inline fun newCompilation(useK2: Boolean, configure: KotlinCompilation.() -> Unit) =
      KotlinCompilation().apply {
        configure()
        verbose = false
        commandLineProcessors = listOf(processor)
        inheritClassPath = true
        languageVersion = if (useK2) "2.0" else "1.9"
        supportsK2 = useK2
      }
}
