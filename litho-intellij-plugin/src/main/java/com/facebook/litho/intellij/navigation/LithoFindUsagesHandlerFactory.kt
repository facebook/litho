/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.intellij.navigation

import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.PsiSearchUtils
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import java.util.function.Function

class LithoFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
  @Suppress("UNCHECKED_CAST")
  private val findGeneratedClass: Function<PsiClass, PsiClass> =
      Function<PsiClass, PsiClass?> { cls -> findGenerated(cls) } as Function<PsiClass, PsiClass>

  private fun findGenerated(cls: PsiClass): PsiClass? {
    if (!LithoPluginUtils.isLithoSpec(cls)) {
      return null
    }
    val specFQN = cls.qualifiedName
    val componentFQN = LithoPluginUtils.getLithoComponentNameFromSpec(specFQN) ?: return null
    return PsiSearchUtils.getInstance().findOriginalClass(cls.project, componentFQN)
  }

  override fun canFindUsages(element: PsiElement): Boolean {
    return GeneratedClassFindUsagesHandler.canFindUsages(element) ||
        SpecMethodFindUsagesHandler.canFindUsages(element)
  }

  override fun createFindUsagesHandler(
      element: PsiElement,
      forHighlightUsages: Boolean,
  ): FindUsagesHandler? {
    if (GeneratedClassFindUsagesHandler.canFindUsages(element)) {
      return GeneratedClassFindUsagesHandler(element, findGeneratedClass)
    }
    if (SpecMethodFindUsagesHandler.canFindUsages(element)) {
      return SpecMethodFindUsagesHandler(element, findGeneratedClass)
    }
    return null
  }
}
