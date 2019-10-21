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

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.model.SpecElementType;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

/** Helper methods to ease the way with Kotlin generated code */
public class KotlinSpecUtils {

  private KotlinSpecUtils() {}

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

  private static String getParameterizedFieldType(String type) {
    final int indexOfStartDiamond = type.indexOf('<');
    final int indexOfEndDiamond = type.indexOf('>');

    return type.substring(indexOfStartDiamond + 1, indexOfEndDiamond);
  }

  public static TypeName getFieldTypeName(SpecModel specModel, TypeName fieldTypeName) {

    final boolean isKotlinSpec = specModel.getSpecElementType() == SpecElementType.KOTLIN_SINGLETON;

    final TypeName varArgTypeName;

    if (isKotlinSpec) {
      final String rawFieldType = fieldTypeName.toString();
      final boolean isNotJvmSuppressWildcardsAnnotated =
          KotlinSpecUtils.isNotJvmSuppressWildcardsAnnotated(rawFieldType);

      /*
       * If it is a JvmSuppressWildcards annotated type on a Kotlin Spec,
       * we should fallback to previous type detection way.
       */
      if (!isNotJvmSuppressWildcardsAnnotated) {
        varArgTypeName = fieldTypeName;
      } else {
        final String parameterizedFieldType =
            KotlinSpecUtils.getParameterizedFieldType(rawFieldType);
        final String[] typeParts = parameterizedFieldType.split(" ");

        // Just in case something has gone pretty wrong
        if (typeParts.length < 3) {
          varArgTypeName = fieldTypeName;
        } else {
          // Calculate appropriate ClassNames
          final int indexOfStartDiamond = rawFieldType.indexOf('<');

          final String fieldType = rawFieldType.substring(0, indexOfStartDiamond);
          final ClassName rawType = KotlinSpecUtils.buildClassName(fieldType);

          final String pureTypeName = typeParts[2];
          final ClassName enclosedType = KotlinSpecUtils.buildClassName(pureTypeName);

          varArgTypeName = ParameterizedTypeName.get(rawType, enclosedType);
        }
      }
    } else {
      // Fallback when it is a Java spec
      varArgTypeName = fieldTypeName;
    }
    return varArgTypeName;
  }
}
