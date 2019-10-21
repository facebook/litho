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

package com.facebook.litho.sections.treeprops;

import javax.annotation.Nullable;

public class LogContext {
  public final String s;

  public LogContext(String s) {
    this.s = s;
  }

  public static LogContext append(@Nullable LogContext t, String s) {
    if (t == null) {
      return new LogContext(s);
    }
    return new LogContext(t.s + ":" + s);
  }

  public String toString() {
    return s;
  }
}
