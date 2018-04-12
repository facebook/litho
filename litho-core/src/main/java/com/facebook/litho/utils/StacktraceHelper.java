/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
