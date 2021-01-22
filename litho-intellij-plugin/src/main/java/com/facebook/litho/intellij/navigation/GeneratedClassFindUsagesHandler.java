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

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.ArrayUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * Adds usages of corresponding Generated class to the search results of the Spec class, and
 * excludes this generated class itself from the places to search.
 */
class GeneratedClassFindUsagesHandler extends FindUsagesHandler {
  private final Function<PsiClass, PsiClass> findGeneratedClass;

  static boolean canFindUsages(PsiElement element) {
    return element instanceof PsiClass && LithoPluginUtils.isLithoSpec((PsiClass) element);
  }

  GeneratedClassFindUsagesHandler(
      PsiElement psiElement, Function<PsiClass, PsiClass> findGeneratedClass) {
    super(psiElement);
    this.findGeneratedClass = findGeneratedClass;
  }

  @Override
  public PsiElement[] getPrimaryElements() {
    final Map<String, String> data = new HashMap<>();
    // Overriden below
    data.put(EventLogger.KEY_RESULT, "fail");
    final PsiElement[] results =
        Optional.of(getPsiElement())
            .filter(PsiClass.class::isInstance)
            .map(PsiClass.class::cast)
            .map(findGeneratedClass)
            .map(
                psiClass -> {
                  data.put(EventLogger.KEY_RESULT, "success");
                  return ArrayUtil.insert(super.getPrimaryElements(), 0, psiClass);
                })
            .orElseGet(super::getPrimaryElements);
    data.put(EventLogger.KEY_TARGET, EventLogger.VALUE_NAVIGATION_TARGET_CLASS);
    data.put(EventLogger.KEY_TYPE, EventLogger.VALUE_NAVIGATION_TYPE_FIND_USAGES);
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_NAVIGATION, data);
    return results;
  }

  @Override
  public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
    FindUsagesOptions findUsagesOptions = super.getFindUsagesOptions(dataContext);
    Optional.of(getPsiElement())
        .filter(PsiClass.class::isInstance)
        .map(PsiClass.class::cast)
        .map(findGeneratedClass)
        .ifPresent(
            generatedCls -> {
              findUsagesOptions.searchScope =
                  new ExcludingScope(
                      findUsagesOptions.searchScope,
                      LithoPluginUtils.getVirtualFile(generatedCls.getContainingFile()));
            });
    return findUsagesOptions;
  }

  /**
   * Scope delegates functions to the underlying {@link #searchScope}, but excludes passed {@link
   * #excluded} from the search.
   */
  static class ExcludingScope extends SearchScope {
    private final SearchScope searchScope;
    private final VirtualFile excluded;

    ExcludingScope(SearchScope searchScope, VirtualFile excluded) {
      this.searchScope = searchScope;
      this.excluded = excluded;
    }

    @Override
    public SearchScope intersectWith(SearchScope scope2) {
      return searchScope.intersectWith(scope2);
    }

    @Override
    public SearchScope union(SearchScope scope) {
      return searchScope.union(scope);
    }

    @Override
    public boolean contains(VirtualFile file) {
      return searchScope.contains(file) && !excluded(file);
    }

    @Override
    public String getDisplayName() {
      return "Litho Spec Usages";
    }

    private boolean excluded(VirtualFile file) {
      return file.equals(excluded);
    }
  }
}
