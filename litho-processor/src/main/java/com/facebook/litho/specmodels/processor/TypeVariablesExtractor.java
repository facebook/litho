/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import java.util.ArrayList;
import java.util.List;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

/**
 * Extracts type variables from the given input.
 */
public class TypeVariablesExtractor {

  /**
   * Get the type variables from the given {@link TypeElement}.
   */
  public static List<TypeVariableName> getTypeVariables(TypeElement typeElement) {
    final List<? extends TypeParameterElement> typeParameters = typeElement.getTypeParameters();
    final int typeParameterCount = typeParameters.size();

    final List<TypeVariableName> typeVariables = new ArrayList<>(typeParameterCount);
    for (TypeParameterElement typeParameterElement : typeParameters) {
      final int boundTypesCount = typeParameterElement.getBounds().size();

      final TypeName[] boundsTypeNames = new TypeName[boundTypesCount];
      for (int i = 0; i < boundTypesCount; i++) {
        boundsTypeNames[i] = TypeName.get(typeParameterElement.getBounds().get(i));
      }

      final TypeVariableName typeVariable =
          TypeVariableName.get(typeParameterElement.getSimpleName().toString(), boundsTypeNames);
      typeVariables.add(typeVariable);
    }

    return typeVariables;
  }
}
