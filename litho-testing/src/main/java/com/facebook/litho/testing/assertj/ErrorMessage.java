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

package com.facebook.litho.testing.assertj;

import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import java.lang.reflect.Field;

/** Utility class for error messages related to the testing api assertions */
public class ErrorMessage {

  /**
   * Prints information about differences between two components
   *
   * @return string containing the basic information about the components and theirs parameters
   */
  static String getCompareComponentsErrorMessage(Component actual, @Nullable Component other) {
    if (other == null) {
      return "Other Component is null";
    }
    String separator = "\n----------\n";
    StringBuilder stringBuilder = new StringBuilder("\n");
    stringBuilder.append("Expected:").append(separator);
    getComponentInfo(actual, stringBuilder);
    stringBuilder.append(separator).append("to be equal to:").append(separator);
    getComponentInfo(other, stringBuilder);
    stringBuilder.append(separator).append("but wasn't");
    return stringBuilder.toString();
  }

  private static void getComponentInfo(Component component, StringBuilder stringBuilder) {
    stringBuilder
        .append(component.getSimpleName())
        .append("\n")
        .append("class: ")
        .append(component.getClass());

    for (Field field : component.getClass().getDeclaredFields()) {
      try {
        final boolean wasAccessible = field.isAccessible();
        if (!wasAccessible) {
          field.setAccessible(true);
        }
        stringBuilder
            .append("\n")
            .append(field.getName())
            .append(" : ")
            .append(field.get(component));
        if (!wasAccessible) {
          field.setAccessible(false);
        }
      } catch (IllegalAccessException e) {
        // Unable to get field by reflection
        continue;
      }
    }
  }
}
