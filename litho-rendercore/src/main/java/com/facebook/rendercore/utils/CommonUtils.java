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

package com.facebook.rendercore.utils;

public class CommonUtils {

  private CommonUtils() {}

  /**
   * This API is used for tracing, and the section names have a char limit of 127. If the class name
   * exceeds that it will be replace by the simple name. In a release build the class name will be
   * minified, so it is unlikely to hit the limit.
   */
  public static String getSectionNameForTracing(Class<?> kclass) {
    final String name = kclass.getName();
    return name.length() > 80 ? kclass.getSimpleName() : "<cls>" + name + "</cls>";
  }

  /** Utility to re-throw exceptions. */
  public static void rethrow(Exception e) {
    if (e instanceof RuntimeException) {
      throw (RuntimeException) e;
    } else {
      throw new RuntimeException(e);
    }
  }
}
