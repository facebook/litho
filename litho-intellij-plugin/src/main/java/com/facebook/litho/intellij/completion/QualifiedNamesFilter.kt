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

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass

class QualifiedNamesFilter(
    private val replacedQualifiedNames: MutableCollection<String>,
    private val insertHandler: InsertHandler<LookupElement>,
) : LookupElementFilter {

  override fun apply(lookupElement: LookupElement, element: PsiClass): LookupElement? {
    val qualifiedName = element.qualifiedName ?: return null
    if (!replacedQualifiedNames.remove(qualifiedName)) {
      return null
    }
    return SpecLookupElement.create(lookupElement, qualifiedName, insertHandler)
  }

  override fun addRemainingCompletions(project: Project, result: CompletionResultSet) {
    for (qualifiedName in replacedQualifiedNames) {
      result.addElement(
          PrioritizedLookupElement.withPriority(
              SpecLookupElement.create(qualifiedName, project, insertHandler),
              Integer.MAX_VALUE.toDouble(),
          ),
      )
    }
  }
}
