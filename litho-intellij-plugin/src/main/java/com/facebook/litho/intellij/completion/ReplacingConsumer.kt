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

import com.intellij.codeInsight.completion.CompletionResult
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.psi.PsiClass
import com.intellij.util.Consumer

/**
 * Consumer adds custom [LookupElement][com.intellij.codeInsight.lookup.LookupElement] items to the
 * given [result].
 *
 * It should be passed before other consumers.
 */
internal class ReplacingConsumer(
    private val result: CompletionResultSet,
    @JvmField val filterElement: LookupElementFilter,
    private val removeIrrelevant: Boolean,
) : Consumer<CompletionResult> {

  override fun consume(completionResult: CompletionResult) {
    val lookupElement = completionResult.lookupElement
    val psiElement = lookupElement.psiElement
    val filterResult =
        if (psiElement is PsiClass) {
          val newLookupElement = filterElement.apply(lookupElement, psiElement)
          if (newLookupElement != null) {
            CompletionUtils.wrap(
                completionResult,
                PrioritizedLookupElement.withPriority(
                    newLookupElement,
                    Integer.MAX_VALUE.toDouble(),
                ),
            )
                ?: if (removeIrrelevant) {
                  null
                } else {
                  completionResult
                }
          } else if (removeIrrelevant) {
            null
          } else {
            completionResult
          }
        } else {
          if (removeIrrelevant) {
            null
          } else {
            completionResult
          }
        }

    if (filterResult != null) {
      result.passResult(filterResult)
    }
  }
}
