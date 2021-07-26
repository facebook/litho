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

package com.facebook.litho.intellij.completion;

import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.util.Consumer;
import java.util.Optional;

/**
 * Consumer adds custom {@link LookupElement} items to the given {@link #result}.
 *
 * <p>It should be passed before other consumers.
 */
class ReplacingConsumer implements Consumer<CompletionResult> {
  private final CompletionResultSet result;
  private final boolean removeIrrelevant;
  public final LookupElementFilter filterElement;

  ReplacingConsumer(
      CompletionResultSet result, LookupElementFilter filterElement, boolean removeIrrelevant) {
    this.result = result;
    this.filterElement = filterElement;
    this.removeIrrelevant = removeIrrelevant;
  }

  @Override
  public void consume(CompletionResult completionResult) {
    LookupElement lookupElement = completionResult.getLookupElement();
    CompletionResult filterResult =
        Optional.ofNullable(lookupElement.getPsiElement())
            .filter(PsiClass.class::isInstance)
            .map(element -> filterElement.apply(lookupElement, (PsiClass) element))
            .map(
                newLookupElement ->
                    CompletionUtils.wrap(
                        completionResult,
                        PrioritizedLookupElement.withPriority(newLookupElement, Integer.MAX_VALUE)))
            .orElse(removeIrrelevant ? null : completionResult);

    if (filterResult != null) {
      result.passResult(filterResult);
    }
  }
}
