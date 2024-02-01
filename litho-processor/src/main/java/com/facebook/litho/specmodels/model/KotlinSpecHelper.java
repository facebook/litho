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

package com.facebook.litho.specmodels.model;

import com.google.common.base.Preconditions;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

/** Helper functions to handle wildcard cases from Kotlin spec files */
public class KotlinSpecHelper {

  public static boolean isKotlinSpec(SpecModel specModel) {
    return specModel.getSpecElementType() == SpecElementType.KOTLIN_SINGLETON
        || specModel.getSpecElementType() == SpecElementType.KOTLIN_CLASS;
  }

  /**
   * Given List&lt;? extends Color&gt; transforms it to List&lt;Color&gt;. Note that if Kotlin spec
   * specifies @JvmSuppressWildcards, then this step is unnecessary.
   */
  public static TypeName maybeRemoveWildcardFromVarArgsIfKotlinSpec(
      SpecModel specModel, TypeName typeName) {
    return maybeRemoveWildcardFromContainerIfKotlinSpec(
        specModel, typeName, 1 /* maxTypeArgumentsSupported */);
  }

  /**
   * Removes all wildcards from a container type, without all number of arguments supported. (See
   * {@link #maybeRemoveWildcardFromVarArgsIfKotlinSpec} for more details).
   */
  public static TypeName maybeRemoveWildcardFromContainerIfKotlinSpec(
      final SpecModel specModel, final TypeName typeName) {
    return maybeRemoveWildcardFromContainerIfKotlinSpec(
        specModel, typeName, Integer.MAX_VALUE /* maxTypeArgumentsSupported */);
  }

  /**
   * Given List&lt;? extends Color&gt; transforms it to List&lt;Color&gt;, for a Kotlin Spec. Note
   * that if Kotlin spec specifies @JvmSuppressWildcards, then this isn't needed.
   *
   * @param maxTypeArgumentsToCleanup the number of type arguments to clean up, where List<T> has 1
   *     and Map<K,T> has 2. If there are more than maxTypeArgumentsToCleanup, we don't do anything.
   */
  public static TypeName maybeRemoveWildcardFromContainerIfKotlinSpec(
      final SpecModel specModel, final TypeName typeName, final int maxTypeArgumentsToCleanup) {
    Preconditions.checkArgument(
        maxTypeArgumentsToCleanup > 0, "Must support updating at least 1 type argument");

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

    final int numTypeArguments = parameterizedTypeName.typeArguments.size();
    if (numTypeArguments > maxTypeArgumentsToCleanup) {
      return typeName;
    }

    final TypeName[] newTypeArguments =
        parameterizedTypeName.typeArguments.toArray(new TypeName[numTypeArguments]);
    for (int i = 0; i < numTypeArguments; i++) {
      final TypeName elementTypeName = newTypeArguments[i];
      if (elementTypeName instanceof WildcardTypeName) {
        newTypeArguments[i] = getBaseTypeIfWildcard(elementTypeName);
      }
    }

    return ParameterizedTypeName.get(parameterizedTypeName.rawType, newTypeArguments);
  }

  /** Given List&lt;? extends Color&gt; returns Color. */
  public static TypeName getBaseTypeIfWildcard(TypeName typeName) {
    if (typeName instanceof WildcardTypeName) {
      return ((WildcardTypeName) typeName).upperBounds.get(0);
    }
    return typeName;
  }
}
