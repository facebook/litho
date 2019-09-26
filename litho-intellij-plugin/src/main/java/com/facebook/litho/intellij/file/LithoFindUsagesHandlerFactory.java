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
package com.facebook.litho.intellij.file;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import java.util.Optional;
import java.util.function.Function;

public class LithoFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

  @Override
  public boolean canFindUsages(PsiElement element) {
    return element instanceof PsiClass && LithoPluginUtils.isLithoSpec((PsiClass) element);
  }

  @Override
  public FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
    return new GeneratedClassFindUsagesHandler(element);
  }

  static class GeneratedClassFindUsagesHandler extends FindUsagesHandler {
    private final Function<PsiClass, Optional<PsiClass>> findComponent;

    GeneratedClassFindUsagesHandler(PsiElement psiElement) {
      this(
          psiElement,
          specCls ->
              LithoPluginUtils.findGeneratedClass(
                  specCls.getQualifiedName(), specCls.getProject()));
    }

    @VisibleForTesting
    GeneratedClassFindUsagesHandler(
        PsiElement psiElement, Function<PsiClass, Optional<PsiClass>> findComponent) {
      super(psiElement);
      this.findComponent = findComponent;
    }

    @Override
    public PsiElement[] getPrimaryElements() {
      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_FIND_USAGES + ".invoke");

      return Optional.of(getPsiElement())
          .filter(PsiClass.class::isInstance)
          .map(PsiClass.class::cast)
          .flatMap(findComponent)
          .map(
              psiClass -> {
                LithoLoggerProvider.getEventLogger()
                    .log(EventLogger.EVENT_FIND_USAGES + ".success");
                return ArrayUtil.insert(super.getPrimaryElements(), 0, psiClass);
              })
          .orElseGet(super::getPrimaryElements);
    }
  }
}
