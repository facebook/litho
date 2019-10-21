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

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
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
