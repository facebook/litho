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
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.util.ArrayUtil
import java.util.function.Function

/**
 * Adds usages of corresponding Generated class to the search results of the Spec class, and
 * excludes this generated class itself from the places to search.
 */
internal class GeneratedClassFindUsagesHandler(
    psiElement: PsiElement,
    private val findGeneratedClass: Function<PsiClass, PsiClass>,
) : FindUsagesHandler(psiElement) {

  override fun getPrimaryElements(): Array<PsiElement> {
    val data = HashMap<String, String>()
    // Overridden below
    data[EventLogger.KEY_RESULT] = "fail"
    val element = psiElement
    val results =
        if (element is PsiClass) {
          val generatedClass = findGeneratedClass.apply(element)
          if (generatedClass != null) {
            data[EventLogger.KEY_RESULT] = "success"
            ArrayUtil.insert(super.getPrimaryElements(), 0, generatedClass)
          } else {
            super.getPrimaryElements()
          }
        } else {
          super.getPrimaryElements()
        }
    data[EventLogger.KEY_TARGET] = EventLogger.VALUE_NAVIGATION_TARGET_CLASS
    data[EventLogger.KEY_TYPE] = EventLogger.VALUE_NAVIGATION_TYPE_FIND_USAGES
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_NAVIGATION, data)
    return results
  }

  override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
    val findUsagesOptions = super.getFindUsagesOptions(dataContext)
    val element = psiElement
    if (element is PsiClass) {
      val generatedCls = findGeneratedClass.apply(element)
      if (generatedCls != null) {
        findUsagesOptions.searchScope =
            ExcludingScope(
                findUsagesOptions.searchScope,
                LithoPluginUtils.getVirtualFile(generatedCls.containingFile),
            )
      }
    }
    return findUsagesOptions
  }

  /**
   * Scope delegates functions to the underlying [searchScope], but excludes passed [excluded] from
   * the search.
   */
  internal class ExcludingScope(
      private val searchScope: SearchScope,
      private val excluded: VirtualFile,
  ) : SearchScope() {

    override fun intersectWith(scope2: SearchScope): SearchScope {
      return searchScope.intersectWith(scope2)
    }

    override fun union(scope: SearchScope): SearchScope {
      return searchScope.union(scope)
    }

    override fun contains(file: VirtualFile): Boolean {
      return searchScope.contains(file) && !excluded(file)
    }

    override fun getDisplayName(): String {
      return "Litho Spec Usages"
    }

    private fun excluded(file: VirtualFile): Boolean {
      return file == excluded
    }
  }

  companion object {
    @JvmStatic
    fun canFindUsages(element: PsiElement): Boolean {
      return element is PsiClass && LithoPluginUtils.isLithoSpec(element)
    }
  }
}
