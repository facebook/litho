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

import com.facebook.litho.intellij.completion.ComponentGenerateUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.DebounceEventLogger;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Annotator that uses {@link LayoutSpecModel} validation to annotate class with error messages.
 * This re-uses Litho compile-time check.
 */
public class LayoutSpecAnnotator implements Annotator {
  private static final EventLogger logger = new DebounceEventLogger(4_000);

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    List<SpecModelValidationError> errors =
        Optional.of(element)
            .filter(PsiClass.class::isInstance)
            .map(PsiClass.class::cast)
            .map(ComponentGenerateUtils::createLayoutModel)
            .map(model -> model.validate(RunMode.normal()))
            .orElse(Collections.emptyList());
    if (errors.size() > 0) {
      logger.log(EventLogger.EVENT_ANNOTATOR);
    }
    errors.forEach(error -> AnnotatorUtils.addError(holder, error));
  }
}
