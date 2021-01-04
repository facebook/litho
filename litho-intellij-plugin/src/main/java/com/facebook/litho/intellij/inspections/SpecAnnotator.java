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
import com.facebook.litho.intellij.redsymbols.FileGenerateUtils;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecModel;
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

/**
 * Annotator that uses {@link LayoutSpecModel} and {@link MountSpecModel} validation to annotate
 * class with error messages. This re-uses Litho compile-time check.
 */
public class SpecAnnotator implements Annotator {
  private static final Logger DEBUG_LOGGER = Logger.getInstance(SpecAnnotator.class);
  private static final EventLogger LOGGER = new DebounceEventLogger(60_000);

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    PsiClass spec = null;
    if (element instanceof PsiClass
        && (LithoPluginUtils.isLayoutSpec((PsiClass) element)
            || LithoPluginUtils.isMountSpec((PsiClass) element))) {
      spec = (PsiClass) element;
    } else if (element instanceof PsiFile) {
      spec =
          LithoPluginUtils.getFirstClass(
                  (PsiFile) element,
                  cls -> (LithoPluginUtils.isLayoutSpec(cls) || LithoPluginUtils.isMountSpec(cls)))
              .orElse(null);
    }

    final String loggingType;
    // null check performed inside isLayoutSpec() and isMountSpec()
    if (LithoPluginUtils.isLayoutSpec(spec)) {
      loggingType = "layout_spec";
    } else if (LithoPluginUtils.isMountSpec(spec)) {
      loggingType = "mount_spec";
    } else {
      return;
    }

    SpecModel specModel = null;
    try {
      // Assuming that this Annotator is called sequentially and not in parallel, we don't do extra
      // work by calling update directly.
      specModel = ComponentGenerateService.getInstance().getOrCreateSpecModel(spec, false);
      FileGenerateUtils.generateClass(spec);
    } catch (Exception e) {
      // Model might contain errors. Proceed to surfacing them.
      DEBUG_LOGGER.debug(e);
    }
    DEBUG_LOGGER.debug(element + " under analysis");

    final List<SpecModelValidationError> errors =
        Optional.ofNullable(specModel)
            .map(model -> model.validate(RunMode.normal()))
            .orElse(Collections.emptyList());
    if (!errors.isEmpty()) {
      final Map<String, String> data = new HashMap<>();
      data.put(EventLogger.KEY_TYPE, loggingType);
      LOGGER.log(EventLogger.EVENT_ANNOTATOR, data);
      errors.forEach(error -> AnnotatorUtils.addError(holder, error));
    }
  }
}
