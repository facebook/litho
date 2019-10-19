/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.annotation.Nullable;

public final class StacktraceHelper {

  /**
   * Format a stack trace in a human-readable format.
   *
   * @param throwable The exception/throwable whose stack trace to format.
   */
  @Nullable
  public static String formatStacktrace(Throwable throwable) {
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);
    String output = null;
    try {
      throwable.printStackTrace(printWriter);
    } finally {
      printWriter.close();
      try {
        output = stringWriter.toString();
        stringWriter.close();
      } catch (final IOException ignored) {
        // This would mean that closing failed which doesn't concern us.
      }
    }

    return output;
  }
}
