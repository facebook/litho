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

package com.facebook.litho.util

import com.facebook.litho.processor.AbstractTestProcessor
import com.tschuchort.compiletesting.SourceFile

private const val TEST_PROCESSOR = "// PROCESSOR:"
private const val EXPECTED_RESULT = "// EXPECTED:"
private const val END = "// END"
private const val FILE_NAME = "// FILE:"

fun testData(path: String): TestData {
  return checkNotNull(TestData::class.java.getResourceAsStream("/resources/$path"))
      .bufferedReader()
      .useLines(::processFile)
}

private fun processFile(lines: Sequence<String>): TestData {
  var processorName = ""
  var fileName = ""
  val processorOutput = mutableListOf<String>()
  val compilerOutput = mutableListOf<String>()
  val sourceFiles = mutableListOf<SourceFile>()
  val source = mutableListOf<String>()
  var current: MutableList<String>? = null
  var trimComment = false
  lines.forEach { line ->
    when {
      line.startsWith(TEST_PROCESSOR) -> {
        check(processorName.isEmpty())
        processorName = line.substringAfter(TEST_PROCESSOR).trim()
        current = processorOutput
        trimComment = true
      }
      line.startsWith(EXPECTED_RESULT) -> {
        current = compilerOutput
        trimComment = true
      }
      line.startsWith(END) -> current = null
      line.startsWith(FILE_NAME) -> {
        if (source.isNotEmpty()) {
          sourceFiles.add(source.toSourceFile(fileName))
          source.clear()
        }
        fileName = line.substringAfter(FILE_NAME).trim()
        current = source
        trimComment = false
      }
      else -> current?.add(if (trimComment) line.substring(3) else line)
    }
  }
  if (processorOutput.isNotEmpty()) processorOutput.add("")
  if (compilerOutput.isNotEmpty()) compilerOutput.add("")
  if (source.isNotEmpty()) sourceFiles.add(source.toSourceFile(fileName))
  check(sourceFiles.isNotEmpty())
  val processor =
      Class.forName("com.facebook.litho.processor.$processorName")
          .getDeclaredConstructor()
          .newInstance() as AbstractTestProcessor
  return TestData(
      processor = processor,
      processorExpectedOutput = processorOutput.joinToString("\n"),
      compilerExpectedOutput = compilerOutput.joinToString("\n"),
      sourceFiles = sourceFiles)
}

private fun MutableList<String>.toSourceFile(fileName: String): SourceFile {
  return when (fileName.substringAfterLast(".")) {
    "kt" -> SourceFile.kotlin(fileName, joinToString("\n"))
    "java" -> SourceFile.java(fileName, joinToString("\n"))
    else -> error("Invalid filename")
  }
}
