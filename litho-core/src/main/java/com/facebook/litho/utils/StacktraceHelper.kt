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

package com.facebook.litho.utils

import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

object StacktraceHelper {
  /**
   * Format a stack trace in a human-readable format.
   *
   * @param throwable The exception/throwable whose stack trace to format.
   */
  @JvmStatic
  fun formatStacktrace(throwable: Throwable): String? {
    val stringWriter = StringWriter()
    val printWriter = PrintWriter(stringWriter)
    var output: String? = null
    try {
      throwable.printStackTrace(printWriter)
    } finally {
      printWriter.close()
      try {
        output = stringWriter.toString()
        stringWriter.close()
      } catch (ignored: IOException) {
        // This would mean that closing failed which doesn't concern us.
      }
    }
    return output
  }
}
