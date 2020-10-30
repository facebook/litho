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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

/** Helper functions to handle wildcard cases from Kotlin spec files */
class KotlinSpecHelper {
  static boolean isKotlinSpec(SpecModel specModel) {
    return specModel.getSpecElementType() == SpecElementType.KOTLIN_SINGLETON;
  }

  /**
   * Given List&lt;? extends Color&gt; transforms it to List&lt;Color&gt;. Note that if Kotlin spec
   * specifies @JvmSuppressWildcards, then this step is unnecessary.
   */
  static TypeName maybeRemoveWildcardFromVarArgsIfKotlinSpec(
      SpecModel specModel, TypeName typeName) {
    if (!isKotlinSpec(specModel)) {
      return typeName;
    }
    if (!(typeName instanceof ParameterizedTypeName)) {
      return typeName;
    }
    final ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) typeName;
    if (parameterizedTypeName.typeArguments.isEmpty()) {
      return typeName;
    }
    final TypeName firstElementTypeName = ((ParameterizedTypeName) typeName).typeArguments.get(0);
    if (firstElementTypeName instanceof WildcardTypeName) {
      return ParameterizedTypeName.get(
          parameterizedTypeName.rawType, getBaseTypeIfWildcard(firstElementTypeName));
    } else {
      return typeName;
    }
  }

  /** Given List&lt;? extends Color&gt; returns Color. */
  static TypeName getBaseTypeIfWildcard(TypeName typeName) {
    if (typeName instanceof WildcardTypeName) {
      return ((WildcardTypeName) typeName).upperBounds.get(0);
    }
    return typeName;
  }
}
