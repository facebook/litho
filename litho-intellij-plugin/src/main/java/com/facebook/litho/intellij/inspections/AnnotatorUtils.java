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

package com.facebook.litho.intellij.inspections;

import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameIdentifierOwner;
import java.util.Optional;

class AnnotatorUtils {
  private AnnotatorUtils() {}

  /** Adds error annotation to the provided holder. */
  static void addError(AnnotationHolder holder, SpecModelValidationError error) {
    PsiElement errorElement = (PsiElement) error.element;
    holder.createErrorAnnotation(
        Optional.of(errorElement)
            .filter(element -> element instanceof PsiClass || element instanceof PsiMethod)
            .map(PsiNameIdentifierOwner.class::cast)
            .map(PsiNameIdentifierOwner::getNameIdentifier)
            .orElse(errorElement),
        error.message);
  }
}
