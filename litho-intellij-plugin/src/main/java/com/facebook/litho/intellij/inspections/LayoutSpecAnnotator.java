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

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.DebounceEventLogger;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Annotator that uses {@link LayoutSpecModel} validation to annotate class with error messages.
 * This re-uses Litho compile-time check.
 */
public class LayoutSpecAnnotator implements Annotator {
  private static final Logger DEBUG_LOGGER = Logger.getInstance(LayoutSpecAnnotator.class);
  private static final EventLogger LOGGER = new DebounceEventLogger(60_000);

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    final PsiClass layoutSpec = getLayoutSpec(element);
    if (layoutSpec == null) return;

    try {
      if (!ComponentGenerateService.getInstance().tryUpdateLayoutComponent(layoutSpec)) {
        return;
      }
    } catch (Exception e) {
      // Model might contain errors. Proceed to surfacing them.
      DEBUG_LOGGER.debug(e);
    }
    DEBUG_LOGGER.debug(element + " under analysis");

    final List<SpecModelValidationError> errors =
        Optional.ofNullable(ComponentGenerateService.getSpecModel(layoutSpec))
            .map(model -> model.validate(RunMode.normal()))
            .orElse(Collections.emptyList());
    if (!errors.isEmpty()) {
      final Map<String, String> data = new HashMap<>();
      data.put(EventLogger.KEY_TYPE, "layout_spec");
      LOGGER.log(EventLogger.EVENT_ANNOTATOR, data);
      errors.forEach(error -> AnnotatorUtils.addError(holder, error));
    }
  }

  @Nullable
  private static PsiClass getLayoutSpec(PsiElement element) {
    if (element instanceof PsiClass && LithoPluginUtils.isLayoutSpec((PsiClass) element)) {
      return (PsiClass) element;
    }
    if (element instanceof PsiFile) {
      return LithoPluginUtils.getFirstLayoutSpec((PsiFile) element).orElse(null);
    }
    return null;
  }
}
