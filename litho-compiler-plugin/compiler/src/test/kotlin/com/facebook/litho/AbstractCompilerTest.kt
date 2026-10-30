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

import com.facebook.kotlin.compilercompat.useK2
import com.facebook.litho.processor.AbstractTestProcessor
import com.facebook.litho.util.testData
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

abstract class AbstractCompilerTest {

  /**
   * Compiles [code] with the Litho plugin installed.
   *
   * [plugins] is the seam for a test that needs a feature configured differently from the one
   * [LithoCompilerPlugins.ALL] ships -- the plugin instance carries its own configuration, so no
   * compiler option is involved.
   */
  fun compile(
      vararg code: SourceFile,
      plugins: List<LithoCompilerPlugin> = LithoCompilerPlugins.ALL,
      processor: AbstractTestProcessor? = null,
  ): JvmCompilationResult {
    val compilation = newCompilation {
      sources = code.asList()
      compilerPluginRegistrars = buildList {
        add(LithoCompilerPluginRegistrar(plugins))
        processor?.let { add(TestComponentRegistrar(it)) }
      }
    }
    return compilation.compile()
  }

  fun runTest(path: String, plugins: List<LithoCompilerPlugin> = LithoCompilerPlugins.ALL) {
    val testData = testData(path)
    val compilationResult = compile(
        *testData.sourceFiles.toTypedArray(),
        plugins = plugins,
        processor = testData.processor,
    )
    val processorActualResult = testData.processor.getProcessorResult()
    val compilerActualResult = testData.processor.extractCompilerResult(compilationResult)
    assertThat(processorActualResult).isEqualTo(testData.processorExpectedOutput)
    assertThat(compilerActualResult).isEqualTo(testData.compilerExpectedOutput)
  }

  private inline fun newCompilation(configure: KotlinCompilation.() -> Unit) =
      KotlinCompilation().apply {
        configure()
        verbose = false
        inheritClassPath = true
        useK2()
        jvmTarget = "11"
        // `@Hook` is an opt-in requirement marker, so a module that calls a hook without the plugin
        // fails to compile. For a real target `litho_hook_identity_decorator` injects the plugin
        // and this flag together; this harness registers the plugin by hand, so it has to pass the
        // flag by hand too. Without it every fixture that calls a hook fails on the opt-in error
        // before the test can assert anything.
        optIn = listOf("com.facebook.litho.annotations.Hook")
      }
}
