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

package com.facebook.samples.litho.documentation.treeprops;

import androidx.annotation.Nullable;

// start_example
public class LogContext {
  public final String tag;

  public LogContext(String tag) {
    this.tag = tag;
  }

  public static LogContext append(@Nullable LogContext parentContext, String tag) {
    if (parentContext == null) {
      return new LogContext(tag);
    }
    return new LogContext(parentContext.tag + ":" + tag);
  }

  public String toString() {
    return tag;
  }
}
// end_example
