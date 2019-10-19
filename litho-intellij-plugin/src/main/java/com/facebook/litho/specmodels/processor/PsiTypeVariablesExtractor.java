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

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiTypeParameter;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;

public class PsiTypeVariablesExtractor {

  public static ImmutableList<TypeVariableName> getTypeVariables(PsiClass psiClass) {
    PsiTypeParameter[] psiTypeParameters = psiClass.getTypeParameters();

    final List<TypeVariableName> typeVariables = new ArrayList<>(psiTypeParameters.length);
    for (PsiTypeParameter psiTypeParameter : psiTypeParameters) {
      final PsiReferenceList extendsList = psiTypeParameter.getExtendsList();
      final PsiClassType[] psiClassTypes = extendsList.getReferencedTypes();

      final TypeName[] boundsTypeNames = new TypeName[psiClassTypes.length];
      for (int i = 0, size = psiClassTypes.length; i < size; i++) {
        boundsTypeNames[i] = PsiTypeUtils.getTypeName(psiClassTypes[i]);
      }

      final TypeVariableName typeVariable =
          TypeVariableName.get(psiTypeParameter.getName(), boundsTypeNames);
      typeVariables.add(typeVariable);
    }

    return ImmutableList.copyOf(typeVariables);
  }
}
