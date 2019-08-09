/*
 * Copyright 2017-present Facebook, Inc.
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

import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/** Extracts prop defaults from the given input. */
public class PsiPropDefaultsExtractor {

  /** Get the prop defaults from the given {@link PsiClass}. */
  public static ImmutableList<PropDefaultModel> getPropDefaults(PsiClass psiClass) {
    final List<PropDefaultModel> propDefaults = new ArrayList<>();

    for (PsiField psiField : psiClass.getFields()) {
      propDefaults.addAll(extractFromField(psiField));
    }

    return ImmutableList.copyOf(propDefaults);
  }

  private static ImmutableList<PropDefaultModel> extractFromField(PsiField psiField) {
    final Annotation propDefaultAnnotation =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiField, PropDefault.class);
    if (propDefaultAnnotation == null) {
      return ImmutableList.of();
    }

    final ResType propDefaultResType = ((PropDefault) propDefaultAnnotation).resType();
    final int propDefaultResId = ((PropDefault) propDefaultAnnotation).resId();

    return ImmutableList.of(
        new PropDefaultModel(
            PsiTypeUtils.getTypeName(psiField.getType()),
            psiField.getName(),
            PsiModifierExtractor.extractModifiers(psiField.getModifierList()),
            psiField,
            propDefaultResType,
            propDefaultResId));
  }
}
