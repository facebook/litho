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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.completion.KotlinCompletionContributor;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtFunctionType;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtParameterList;
import org.jetbrains.kotlin.psi.KtPrimaryConstructor;
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry;
import org.jetbrains.kotlin.psi.KtSuperTypeList;
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
      if (psiElement == null) {
        return;
      }
      LookupElement lookupElement;
      if (checkIsComponentWrapper(psiElement)) {
        final KtNamedFunction lithoKotlinFunction = (KtNamedFunction) psiElement;
        final String lookupString = createKotlinCompletionString(lithoKotlinFunction);
        final String name = lithoKotlinFunction.getName();
        if (name == null) {
          return;
        }
        lookupElement =
            createKotlinCompletionLookupElement(lithoKotlinFunction, lookupString, element, name);
      } else if (checkIsKComponent(psiElement)) {
        final KtClass lithoKotlinClass = (KtClass) psiElement;
        final @Nullable String lookupString =
            createKotlinCompletionStringForKComponent(lithoKotlinClass);
        if (lookupString == null) {
          return;
        }
        final String name = lithoKotlinClass.getName();
        if (name == null) {
          return;
        }
        lookupElement =
            createKotlinCompletionLookupElement(lithoKotlinClass, lookupString, element, name);
      } else {
        return;
      }
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

  static boolean checkIsKComponent(@Nullable PsiElement psiElement) {
    if (!(psiElement instanceof KtClass)) {
      return false;
    }
    final KtClass ktClass = (KtClass) psiElement;
    final KtSuperTypeList superTypeList = ktClass.getSuperTypeList();
    if (superTypeList == null) {
      return false;
    }
    final @Nullable KtSuperTypeCallEntry ktSuperTypeCallEntry =
        PsiTreeUtil.getChildOfType(superTypeList, KtSuperTypeCallEntry.class);

    if (ktSuperTypeCallEntry == null) {
      return false;
    }
    return ktSuperTypeCallEntry.getText().equals("KComponent()");
  }

  static boolean checkIsComponentWrapper(@Nullable PsiElement psiElement) {
    if (!(psiElement instanceof KtNamedFunction)) { // Kotlin Litho components are functions
      return false;
    }
    final List<KtTypeReference> referenceTypes =
        PsiTreeUtil.getChildrenOfTypeAsList(psiElement, KtTypeReference.class); // 2 Types:
    if (referenceTypes.size() < 2) {
      return false;
    }
    return "ComponentScope".equals(referenceTypes.get(0).getText());
  }

  static String createKotlinCompletionString(KtNamedFunction lithoKotlinFunction) {
    final List<String> parameters =
        lithoKotlinFunction.getValueParameters().stream()
            .filter(param -> !param.hasDefaultValue())
            .map(param -> param.getName() + " = ")
            .collect(Collectors.toList());
    return lithoKotlinFunction.getName() + "(" + String.join(", ", parameters) + " )";
  }

  static @Nullable String createKotlinCompletionStringForKComponent(KtClass lithoKotlinClass) {
    final KtPrimaryConstructor ktPrimaryConstructor =
        PsiTreeUtil.getChildOfType(lithoKotlinClass, KtPrimaryConstructor.class);
    if (ktPrimaryConstructor == null) {
      return null;
    }
    final AtomicBoolean addKotlinLambda = new AtomicBoolean(false);
    final KtParameterList ktParameterList =
        PsiTreeUtil.getChildOfType(ktPrimaryConstructor, KtParameterList.class);
    if (ktParameterList == null) {
      return null;
    }
    final List<KtParameter> parameterList = ktParameterList.getParameters();
    if (parameterList.isEmpty()) {
      return null;
    }
    final List<String> parameters =
        parameterList.stream()
            .filter(param -> !param.hasDefaultValue())
            .map(param -> param.getName() + " = ")
            .collect(Collectors.toList());

    final @Nullable KtTypeReference typeElement =
        parameterList.get(parameterList.size() - 1).getTypeReference();
    if (typeElement != null && typeElement.getTypeElement() instanceof KtFunctionType) {
      addKotlinLambda.set(true);
      // remove final param as we turn it into lambda expression
      parameters.remove(parameters.size() - 1);
    }

    final String completionString =
        lithoKotlinClass.getName() + "(" + String.join(", ", parameters) + ")";
    if (addKotlinLambda.get()) {
      return completionString + " {}";
    }
    return completionString;
  }

  static LookupElement createKotlinCompletionLookupElement(
      PsiElement lithoKotlinElement, String joinedParameters, LookupElement element, String name) {
    final LookupElement lookupElement =
        LookupElementBuilder.create(joinedParameters)
            .withBoldness(true)
            .withPresentableText(name)
            .appendTailText(joinedParameters.replaceFirst(name, ""), true)
            .withLookupString(name)
            .withTypeText("Litho " + name)
            .withIcon(lithoKotlinElement.getIcon(1))
            .withInsertHandler(
                (context, lookupItem) -> {
                  // We use this insert handler to get the import statement added automatically. We
                  // then delete the inserted lookup element and add out own Completion result.
                  element.handleInsert(context);
                  context
                      .getDocument()
                      .deleteString(context.getStartOffset(), context.getTailOffset());
                  context
                      .getDocument()
                      .insertString(context.getTailOffset(), lookupItem.getLookupString());
                  context.commitDocument();
                  context.getEditor().getCaretModel().moveToOffset(context.getTailOffset() - 1);
                });
    return PrioritizedLookupElement.withPriority(lookupElement, Integer.MAX_VALUE);
  }
}
