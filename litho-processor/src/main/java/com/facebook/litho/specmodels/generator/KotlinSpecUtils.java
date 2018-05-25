/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

import com.squareup.javapoet.ClassName;

/*
* Helper methods to ease the way with Kotlin generated code
* */
public class KotlinSpecUtils {

  public static boolean isNotJvmSuppressWildcardsAnnotated(String type) {
    return type.contains("extends");
  }

  public static ClassName buildClassName(String typeName) {
    final int lastIndexOfDotForPackage = typeName.lastIndexOf('.');
    final int lastIndexOfDotForClass = typeName.lastIndexOf('.') + 1;

    final String packageName = typeName.substring(0, lastIndexOfDotForPackage);
    final String className = typeName.substring(lastIndexOfDotForClass);

    return ClassName.get(packageName, className);
  }

  public static String getParameterizedFieldType(String type) {
    final int indexOfStartDiamond = type.indexOf("<");
    final int indexOfEndDiamond = type.indexOf(">");

    return type.substring(indexOfStartDiamond + 1, indexOfEndDiamond);
  }
}
