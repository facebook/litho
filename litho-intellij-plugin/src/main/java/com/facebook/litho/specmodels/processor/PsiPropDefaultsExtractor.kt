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

import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.PropDefaultModel
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField

/** Extracts prop defaults from the given input. */
object PsiPropDefaultsExtractor {

  /** Get the prop defaults from the given [PsiClass]. */
  @JvmStatic
  fun getPropDefaults(psiClass: PsiClass): ImmutableList<PropDefaultModel> {
    val propDefaults = ArrayList<PropDefaultModel>()

    for (psiField in psiClass.fields) {
      propDefaults.addAll(extractFromField(psiField))
    }

    return ImmutableList.copyOf(propDefaults)
  }

  @Suppress("UNCHECKED_CAST")
  private fun extractFromField(psiField: PsiField): ImmutableList<PropDefaultModel> {
    val propDefaultAnnotation: PropDefault =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(
            psiField,
            PropDefault::class.java as Class<java.lang.annotation.Annotation>,
        ) as? PropDefault ?: return ImmutableList.of()

    val propDefaultResType = propDefaultAnnotation.resType
    val propDefaultResId = propDefaultAnnotation.resId

    val modifierList = psiField.modifierList ?: return ImmutableList.of()
    return ImmutableList.of(
        PropDefaultModel(
            PsiTypeUtils.getTypeName(psiField.type),
            psiField.name,
            PsiModifierExtractor.extractModifiers(modifierList),
            psiField,
            propDefaultResType,
            propDefaultResId,
            PropDefaultModel.AccessorType.FIELD,
        ),
    )
  }
}
