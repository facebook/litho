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
import com.facebook.litho.specmodels.model.FieldModel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.squareup.javapoet.FieldSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Extractor provides {@link FieldModel}s. */
class PsiFieldsExtractor {

  /**
   * @param psiClass to extract fields from. It will not be modified.
   * @return the list of {@link FieldModel}s of the given {@link PsiClass} or empty list if there
   *     are no fields.
   */
  static ImmutableList<FieldModel> extractFields(PsiClass psiClass) {
    return Optional.of(psiClass)
        .map(PsiClass::getFields)
        .map(Arrays::stream)
        .map(
            fields ->
                fields
                    .filter(Objects::nonNull)
                    .map(PsiFieldsExtractor::createFieldModel)
                    .collect(Collectors.toCollection(ArrayList::new)))
        .map(ImmutableList::copyOf)
        .orElse(ImmutableList.of());
  }

  // package access to be used in lambda
  static FieldModel createFieldModel(PsiField psiField) {
    return new FieldModel(
        FieldSpec.builder(
                PsiTypeUtils.getTypeName(psiField.getType()),
                psiField.getName(),
                PsiModifierExtractor.extractModifiers(psiField))
            .build(),
        psiField);
  }
}
