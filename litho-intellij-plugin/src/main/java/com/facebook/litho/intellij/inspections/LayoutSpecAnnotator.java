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

import com.facebook.litho.intellij.IntervalLogger;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.completion.ComponentGenerateUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.DebounceEventLogger;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.diagnostic.Logger;
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
  private static final IntervalLogger DEBUG_LOGGER =
      new IntervalLogger(Logger.getInstance(LayoutSpecAnnotator.class));
  private static final EventLogger LOGGER = new DebounceEventLogger(60_000);

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    DEBUG_LOGGER.logStep("start " + element);
    if (!(element instanceof PsiClass)) return;

    final PsiClass cls = (PsiClass) element;
    if (!LithoPluginUtils.isLayoutSpec(cls)) return;

    final List<SpecModelValidationError> errors =
        Optional.ofNullable(ComponentGenerateUtils.createLayoutModel(cls))
            .map(model -> model.validate(RunMode.normal()))
            .orElse(Collections.emptyList());
    if (!errors.isEmpty()) {
      LOGGER.log(EventLogger.EVENT_ANNOTATOR + ".layout_spec");
      errors.forEach(error -> AnnotatorUtils.addError(holder, error));
    }
    DEBUG_LOGGER.logStep("end " + element);
  }
}
