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

package com.facebook.litho.intellij.completion

import com.facebook.litho.intellij.LithoClassNames
import com.facebook.litho.intellij.PsiSearchUtils
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass

/** Lookup item renders an element with Litho type. */
internal class SpecLookupElement
private constructor(
    delegate: LookupElement,
    private val insertHandler: InsertHandler<LookupElement>,
) : LookupElementDecorator<LookupElement>(delegate) {

  private constructor(
      typeCls: PsiClass,
      insertHandler: InsertHandler<LookupElement>,
  ) : this(JavaClassNameCompletionContributor.createClassLookupItem(typeCls, true), insertHandler)

  override fun handleInsert(context: InsertionContext) {
    super.handleInsert(context)
    insertHandler.handleInsert(context, this)
  }

  override fun renderElement(presentation: LookupElementPresentation) {
    super.renderElement(presentation)
    presentation.typeText = "Litho"
    presentation.isTypeGrayed = true
  }

  override fun isCaseSensitive(): Boolean {
    return false
  }

  companion object {
    private val CACHE: MutableMap<String, LookupElement> = HashMap(20)

    /**
     * @param qualifiedName the name of the class to create lookup
     * @param project to find the lookup annotation class
     * @param insertHandler adds custom actions to the insert handling
     * @return new [LookupElement] or cached instance if it was created previously
     * @throws com.intellij.util.IncorrectOperationException if the qualifiedName does not specify a
     *   valid type
     */
    @JvmStatic
    fun create(
        qualifiedName: String,
        project: Project,
        insertHandler: InsertHandler<LookupElement>,
    ): LookupElement {
      val cached = CACHE[qualifiedName]
      if (cached != null) {
        return cached
      }
      val typeCls = PsiSearchUtils.getInstance().findClass(project, qualifiedName)
      if (typeCls != null) {
        val lookupElement = SpecLookupElement(typeCls, insertHandler)
        CACHE[qualifiedName] = lookupElement
        return lookupElement
      }
      // This is a dummy class, we don't want to cache it.
      val dummyCls =
          JavaPsiFacade.getInstance(project)
              .elementFactory
              .createClass(LithoClassNames.shortName(qualifiedName))
      return SpecLookupElement(dummyCls, insertHandler)
    }

    @JvmStatic
    fun create(
        delegate: LookupElement,
        qualifiedName: String,
        insertHandler: InsertHandler<LookupElement>,
    ): LookupElement {
      val lookupElement = SpecLookupElement(delegate, insertHandler)
      CACHE[qualifiedName] = lookupElement
      return lookupElement
    }
  }
}
