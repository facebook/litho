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

package com.facebook.litho.processor

import com.tschuchort.compiletesting.JvmCompilationResult
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.ir.declarations.IrFile

/**
 * Base class for all processors that are used in self-checking compiler tests.
 *
 * The [AbstractTestProcessor] analyses source code IR & compilation outputs and transforms them
 * into expected results that can be verified against. Implementations of this class should override
 * [analyse] to define their own implementation of the source transformation accordingly. They can
 * also override [extractCompilerResult] to define a custom logic of extracting the compilation
 * result from the [JvmCompilationResult] object.
 *
 * Note that the default implementation of [extractCompilerResult] simply sanitizes the compiler
 * output, removing unnecessary lines and replacing temporary file names with a generic placeholder
 * to ensure reproducibility.
 *
 * As a convention, implementations of this class should be kept in the same package as the
 * [AbstractTestProcessor] itself. This is to ensure that the compiler can find the processor
 * implementation class when it is invoked.
 */
abstract class AbstractTestProcessor {

  /**
   * This class is used to pass necessary data to the [AbstractTestProcessor] for analysis.
   *
   * It contains the intermediate representation [file] that will be analysed. This file provides
   * necessary information about the structure of the source code. When needed, a [context] object
   * that provides access to useful APIs within the compilation unit is also exposed.
   */
  class AnalysisData(val file: IrFile, val context: IrPluginContext)

  private val processorOutput = StringBuilder()

  /**
   * Analyses the given [data], extracts necessary information as the case may be, and serializes it
   * into text format which is then written into a buffer.
   */
  fun analyse(data: AnalysisData) {
    processorOutput.analyse(data)
  }

  /**
   * Processes the given [data] and serializes the result into the receiving [StringBuilder].
   *
   * This method is invoked during the compilation phase to analyse the source code IR and extract
   * necessary information for the test. The implementation of this method should be kept as simple
   * as possible, as it is not intended to perform any complex logic.
   */
  abstract fun StringBuilder.analyse(data: AnalysisData)

  /**
   * Returns the processor analysis result.
   *
   * This is the result that will be compared against the expected result.
   */
  fun getProcessorResult(): String = processorOutput.toString()

  /**
   * Extracts the compiler result from the given [JvmCompilationResult].
   *
   * The default implementation simply sanitizes the compiler output, removing unnecessary lines and
   * replacing temporary file names with a generic placeholder to ensure reproducibility.
   */
  @OptIn(ExperimentalCompilerApi::class)
  open fun extractCompilerResult(compilationResult: JvmCompilationResult): String {
    return extractCompilerMessage(compilationResult)
  }

  companion object {
    private val REGEX_FILENAME = "file:///tmp/[-\\w]+/sources/(.+).(kt|java)".toRegex()

    @OptIn(ExperimentalCompilerApi::class)
    fun extractCompilerMessage(result: JvmCompilationResult): String {
      return result.messages
          .splitToSequence("\n")
          .dropWhile { it.startsWith("w: Default scripting plugin is disabled") }
          .joinToString("\n") { it.replace(REGEX_FILENAME, "file:///$1.$2") }
    }
  }
}
