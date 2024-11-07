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

import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

abstract class AbstractCompilerTest {
  companion object {
    @JvmField val PLUGIN_ENABLED = PluginOption("com.facebook.litho.compiler", "enabled", "true")
  }

  fun compile(
      code: SourceFile,
      options: List<PluginOption> = listOf(PLUGIN_ENABLED),
      useK2: Boolean = false
  ): CompilationResult {
    val compilation =
        KotlinCompilation().apply {
          sources = listOf(code)
          compilerPluginRegistrars = listOf(LithoComponentRegistrar())
          commandLineProcessors = listOf(LithoCommandLineProcessor())
          pluginOptions = options
          inheritClassPath = true
          languageVersion = if (useK2) "2.0" else "1.9"
          supportsK2 = useK2
        }
    return compilation.compile()
  }
}
