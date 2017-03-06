// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.processor;

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
