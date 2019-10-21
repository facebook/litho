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

package com.facebook.litho.intellij.file;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.ArrayUtil;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public class LithoFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

  @Override
  public boolean canFindUsages(PsiElement element) {
    return element instanceof PsiClass && LithoPluginUtils.isLithoSpec((PsiClass) element);
  }

  @Override
  public FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
    return new GeneratedClassFindUsagesHandler(element);
  }

  /**
   * Adds usages of corresponding Generated class to the search results of the Spec class, and
   * excludes this generated class itself from the places to search.
   */
  static class GeneratedClassFindUsagesHandler extends FindUsagesHandler {
    private final Function<PsiClass, Optional<PsiClass>> findGeneratedClass;

    GeneratedClassFindUsagesHandler(PsiElement psiElement) {
      this(
          psiElement,
          specCls ->
              LithoPluginUtils.findGeneratedClass(
                  specCls.getQualifiedName(), specCls.getProject()));
    }

    @VisibleForTesting
    GeneratedClassFindUsagesHandler(
        PsiElement psiElement, Function<PsiClass, Optional<PsiClass>> findGeneratedClass) {
      super(psiElement);
      this.findGeneratedClass = findGeneratedClass;
    }

    @Override
    public PsiElement[] getPrimaryElements() {
      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_FIND_USAGES + ".invoke");

      return Optional.of(getPsiElement())
          .filter(PsiClass.class::isInstance)
          .map(PsiClass.class::cast)
          .flatMap(findGeneratedClass)
          .map(
              psiClass -> {
                LithoLoggerProvider.getEventLogger()
                    .log(EventLogger.EVENT_FIND_USAGES + ".success");
                return ArrayUtil.insert(super.getPrimaryElements(), 0, psiClass);
              })
          .orElseGet(super::getPrimaryElements);
    }

    @Override
    public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
      FindUsagesOptions findUsagesOptions = super.getFindUsagesOptions(dataContext);
      PsiElement searchTarget = getPsiElement();
      if (!(searchTarget instanceof PsiClass)) {
        return findUsagesOptions;
      }
      Optional<PsiClass> generatedCls = findGeneratedClass.apply((PsiClass) searchTarget);
      if (!generatedCls.isPresent()) {
        return findUsagesOptions;
      }
      findUsagesOptions.searchScope =
          new ExcludingScope(findUsagesOptions.searchScope, generatedCls.get());

      return findUsagesOptions;
    }

    /**
     * Scope delegates functions to the underlying {@link #searchScope}, but excludes passed {@link
     * #excluded} from the search.
     */
    static class ExcludingScope extends SearchScope {
      private final SearchScope searchScope;
      private final VirtualFile excluded;

      ExcludingScope(SearchScope searchScope, PsiClass excluded) {
        this.searchScope = searchScope;
        this.excluded = excluded.getContainingFile().getVirtualFile();
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
        return excluded.equals(file);
      }
    }
  }
}
