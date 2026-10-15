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

package com.facebook.litho.specmodels.processor

import com.facebook.litho.specmodels.internal.ImmutableList
import com.intellij.psi.PsiClass
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName

object PsiTypeVariablesExtractor {

  @JvmStatic
  fun getTypeVariables(psiClass: PsiClass): ImmutableList<TypeVariableName> {
    val psiTypeParameters = psiClass.typeParameters

    val typeVariables = ArrayList<TypeVariableName>(psiTypeParameters.size)
    for (psiTypeParameter in psiTypeParameters) {
      val extendsList = psiTypeParameter.extendsList
      val psiClassTypes = extendsList.referencedTypes

      val boundsTypeNames = arrayOfNulls<TypeName>(psiClassTypes.size)
      for (i in psiClassTypes.indices) {
        boundsTypeNames[i] = PsiTypeUtils.getTypeName(psiClassTypes[i])
      }

      val typeVariable = TypeVariableName.get(psiTypeParameter.name, *boundsTypeNames)
      typeVariables.add(typeVariable)
    }

    return ImmutableList.copyOf(typeVariables)
  }
}
