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

package com.facebook.litho.specmodels.processor;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

/** Extracts type variables from the given input. */
public class TypeVariablesExtractor {

  /** Get the type variables from the given {@link TypeElement}. */
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
