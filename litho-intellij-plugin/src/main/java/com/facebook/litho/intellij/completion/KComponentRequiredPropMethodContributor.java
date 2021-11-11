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

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ProcessingContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.kotlin.idea.completion.KotlinCompletionContributor;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtTypeReference;

public class KComponentRequiredPropMethodContributor extends CompletionContributor {

  private static final CompletionContributor kotlinCompletionContributor =
      new KotlinCompletionContributor();

  public KComponentRequiredPropMethodContributor() {
    extend(
        CompletionType.BASIC,
        CompletionUtils.IN_KT_EXPR,
        KComponentRequiredPropMethodProvider.INSTANCE);
  }

  public static class KComponentRequiredPropMethodProvider
      extends CompletionProvider<CompletionParameters> {
    static final CompletionProvider<CompletionParameters> INSTANCE =
        new KComponentRequiredPropMethodProvider();

    @Override
    protected void addCompletions(
        CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
      DummyCompletionResultSet dummyCompletionResultSet =
          new DummyCompletionResultSet(
              result.getPrefixMatcher(), element -> {}, kotlinCompletionContributor);
      kotlinCompletionContributor.fillCompletionVariants(parameters, dummyCompletionResultSet);

      dummyCompletionResultSet.completionLookupElements.forEach(result::addElement);
    }
  }

  private static class DummyCompletionResultSet extends CompletionResultSet {

    private final List<LookupElement> completionLookupElements = new ArrayList<>();

    protected DummyCompletionResultSet(
        PrefixMatcher prefixMatcher,
        Consumer<? super CompletionResult> consumer,
        CompletionContributor contributor) {
      super(prefixMatcher, consumer, contributor);
    }

    @Override
    public void addElement(LookupElement element) {
      final PsiElement psiElement = element.getPsiElement();
      if (!(psiElement instanceof KtNamedFunction)) { // Kotlin Litho components are functions
        return;
      }

      final List<KtTypeReference> referenceTypes =
          PsiTreeUtil.getChildrenOfTypeAsList(psiElement, KtTypeReference.class); // 2 Types:

      if (referenceTypes.size() < 2) {
        return;
      }
      if (!"ComponentScope".equals(referenceTypes.get(0).getText())) {
        return;
      }
      final KtNamedFunction lithoKotlinFunction = (KtNamedFunction) psiElement;
      final String lookupString = createKotlinCompletionString(lithoKotlinFunction);
      final LookupElement lookupElement =
          createKotlinCompletionLookupElement(lithoKotlinFunction, lookupString, element);
      completionLookupElements.add(lookupElement);
    }

    @Override
    public CompletionResultSet withPrefixMatcher(PrefixMatcher matcher) {
      return this;
    }

    @Override
    public CompletionResultSet withPrefixMatcher(String prefix) {
      return this;
    }

    @Override
    public CompletionResultSet withRelevanceSorter(CompletionSorter sorter) {
      return this;
    }

    @Override
    public void addLookupAdvertisement(String text) {}

    @Override
    public CompletionResultSet caseInsensitive() {
      return this;
    }

    @Override
    public void restartCompletionOnPrefixChange(ElementPattern<String> prefixCondition) {}

    @Override
    public void restartCompletionWhenNothingMatches() {}
  }

  protected static String createKotlinCompletionString(KtNamedFunction lithoKotlinFunction) {
    final List<String> parameters =
        lithoKotlinFunction.getValueParameters().stream()
            .filter(param -> !param.hasDefaultValue())
            .map(param -> param.getName() + " = ")
            .collect(Collectors.toList());
    return String.join(", ", parameters);
  }

  static LookupElement createKotlinCompletionLookupElement(
      KtNamedFunction lithoKotlinFunction, String joinedParameters, LookupElement element) {
    final String functionName = lithoKotlinFunction.getName();
    final LookupElement lookupElement =
        LookupElementBuilder.create(joinedParameters)
            .withBoldness(true)
            .withPresentableText(functionName)
            .appendTailText("(" + joinedParameters + ")", true)
            .withLookupString(functionName)
            .withTypeText("Litho " + functionName)
            .withIcon(lithoKotlinFunction.getIcon(1))
            .withInsertHandler(
                (context, lookupItem) -> {
                  // We use this insert handler to get the import statement added automatically. We
                  // then delete the inserted lookup element and add out own Completion result.
                  element.handleInsert(context);
                  context
                      .getDocument()
                      .insertString(context.getTailOffset() - 1, lookupItem.getLookupString());
                  context.commitDocument();
                  context.getEditor().getCaretModel().moveToOffset(context.getTailOffset() - 1);
                });
    return PrioritizedLookupElement.withPriority(lookupElement, Integer.MAX_VALUE);
  }
}
