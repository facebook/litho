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

package com.facebook.litho.intellij.navigation;

import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Adds usages of corresponding Generated class's methods to the search results of the Spec class's
 * method.
 */
public class SpecMethodFindUsagesHandler extends FindUsagesHandler {
  private final Function<PsiClass, PsiClass> findGeneratedClass;

  static boolean canFindUsages(PsiElement element) {
    return element instanceof PsiMethod
        && ((PsiMethod) element).getModifierList().hasModifierProperty(PsiModifier.STATIC);
  }

  SpecMethodFindUsagesHandler(
      PsiElement psiElement, Function<PsiClass, PsiClass> findGeneratedClass) {
    super(psiElement);
    this.findGeneratedClass = findGeneratedClass;
  }

  @Override
  public PsiElement[] getPrimaryElements() {
    return Optional.of(getPsiElement())
        .filter(PsiMethod.class::isInstance)
        .map(PsiMethod.class::cast)
        .map(this::findComponentMethods)
        .map(
            methods -> {
              final Map<String, String> data = new HashMap<>();
              data.put(EventLogger.KEY_TARGET, "method");
              data.put(EventLogger.KEY_TYPE, EventLogger.VALUE_NAVIGATION_TYPE_FIND_USAGES);
              LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_NAVIGATION, data);
              return ArrayUtil.mergeArrays(methods, super.getPrimaryElements());
            })
        .orElseGet(super::getPrimaryElements);
  }

  private PsiMethod[] findComponentMethods(PsiMethod method) {
    return Optional.ofNullable(PsiTreeUtil.findFirstParent(method, PsiClass.class::isInstance))
        .map(PsiClass.class::cast)
        .map(findGeneratedClass)
        .map(component -> component.findMethodsByName(method.getName(), true))
        .orElse(PsiMethod.EMPTY_ARRAY);
  }
}
