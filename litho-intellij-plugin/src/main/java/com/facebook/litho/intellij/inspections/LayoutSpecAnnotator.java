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
package com.facebook.litho.intellij.inspections;

import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.facebook.litho.specmodels.processor.PsiLayoutSpecModelFactory;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameIdentifierOwner;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Annotator that uses {@link com.facebook.litho.specmodels.model.LayoutSpecModel} validation to
 * annotate class with error messages. This re-uses Litho implementation done during compile-time
 * check.
 */
public class LayoutSpecAnnotator implements Annotator {

  private static final PsiLayoutSpecModelFactory MODEL_FACTORY = new PsiLayoutSpecModelFactory();

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    List<SpecModelValidationError> errors =
        Optional.of(element)
            .filter(PsiClass.class::isInstance)
            .map(PsiClass.class::cast)
            .map(LayoutSpecAnnotator::createLayoutModel)
            .map(model -> model.validate(RunMode.normal()))
            .orElse(Collections.emptyList());
    if (errors.size() > 0) {
      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_ANNOTATOR);
    }
    errors.forEach(error -> addError(holder, error));
  }

  @Nullable
  static LayoutSpecModel createLayoutModel(PsiClass cls) {
    return MODEL_FACTORY.createWithPsi(cls.getProject(), cls, null);
  }

  private void addError(AnnotationHolder holder, SpecModelValidationError error) {
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
