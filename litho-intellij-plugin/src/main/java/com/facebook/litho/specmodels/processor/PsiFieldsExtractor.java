/*
 * Copyright 2019-present Facebook, Inc.
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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.FieldModel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierList;
import com.squareup.javapoet.FieldSpec;
import java.util.Arrays;
import java.util.Optional;
import javax.lang.model.element.Modifier;

class PsiFieldsExtractor {
  static ImmutableList<FieldModel> extractFields(PsiClass psiClass) {
    return Optional.of(psiClass)
        .map(PsiClass::getFields)
        .map(Arrays::stream)
        .map(
            fields ->
                fields
                    .map(
                        psiField -> {
                          Modifier[] empty = new Modifier[0];
                          PsiModifierList modifierList = psiField.getModifierList();
                          Modifier[] modifiers =
                              modifierList == null
                                  ? empty
                                  : PsiProcessingUtils.extractModifiers(modifierList)
                                      .toArray(empty);
                          return new FieldModel(
                              FieldSpec.builder(
                                      PsiTypeUtils.getTypeName(psiField.getType()),
                                      psiField.getName(),
                                      modifiers)
                                  .build(),
                              psiField);
                        })
                    .toArray(FieldModel[]::new))
        .map(ImmutableList::of)
        .orElse(ImmutableList.of());
  }
}
