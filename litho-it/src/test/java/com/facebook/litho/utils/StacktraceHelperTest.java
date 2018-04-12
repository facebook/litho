/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.utils;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

public class StacktraceHelperTest {
  @Test
  public void testFormatStackTrace() {
    // Create a real stacktrace.
    final Throwable t;
    try {
      throw new Exception("Woops!");
    } catch (final Exception e) {
      t = e;
    }

    final String s = StacktraceHelper.formatStacktrace(t);
    assertThat(s)
        .startsWith("java.lang.Exception: Woops!")
        .contains(
            "at com.facebook.litho.utils.StacktraceHelperTest.testFormatStackTrace(StacktraceHelperTest.java:");
  }
}
