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

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Offers completion for the {@code @Prop} and {@code @State} method parameters in the Litho Spec
 * class.
 */
public class StatePropCompletionContributor extends CompletionContributor {
  public StatePropCompletionContributor() {
    extend(CompletionType.BASIC, codeReferencePattern(), typeCompletionProvider());
  }

  private static ElementPattern<? extends PsiElement> codeReferencePattern() {
    return PlatformPatterns.psiElement(PsiIdentifier.class)
        .withParent(PsiJavaCodeReferenceElement.class)
        .withLanguage(JavaLanguage.INSTANCE);
  }

  private static CompletionProvider<CompletionParameters> typeCompletionProvider() {
    return new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(
          @NotNull CompletionParameters completionParameters,
          ProcessingContext processingContext,
          @NotNull CompletionResultSet completionResultSet) {
        PsiElement element = completionParameters.getPosition();

        // Method parameter type in the Spec class
        // PsiIdentifier -> PsiJavaCodeReferenceElement -> PsiTypeElement -> PsiMethod -> PsiClass
        PsiElement typeElement = PsiTreeUtil.getParentOfType(element, PsiTypeElement.class);
        if (typeElement == null) {
          return;
        }
        PsiMethod containingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (containingMethod == null) {
          return;
        }
        PsiClass cls = containingMethod.getContainingClass();
        if (!LithoPluginUtils.isLithoSpec(cls)) {
          return;
        }

        // @Prop or @State annotation
        PsiModifierList parameterModifiers =
            PsiTreeUtil.getPrevSiblingOfType(typeElement, PsiModifierList.class);
        if (parameterModifiers == null) {
          return;
        }
        if (parameterModifiers.findAnnotation(Prop.class.getName()) != null) {
          addCompletionResult(
              completionResultSet, containingMethod, cls.getMethods(), LithoPluginUtils::isProp);
        } else if (parameterModifiers.findAnnotation(State.class.getName()) != null) {
          addCompletionResult(
              completionResultSet, containingMethod, cls.getMethods(), LithoPluginUtils::isState);
        }
      }
    };
  }

  // Package-private to be accessed from the anonymous inner class
  static void addCompletionResult(
      @NotNull CompletionResultSet completionResultSet,
      PsiMethod currentMethod,
      PsiMethod[] allMethods,
      Predicate<PsiParameter> annotationCheck) {
    // Don't propose completion with current method parameters
    Set<String> excludingParameters =
        Stream.of(currentMethod.getParameterList().getParameters())
            .filter(annotationCheck)
            .map(PsiParameter::getName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    LithoPluginUtils.getPsiParameterStream(currentMethod, allMethods)
        .filter(annotationCheck)
        .filter(parameter -> !excludingParameters.contains(parameter.getName()))
        .map(StatePropCompletionContributor::createCompletionResult)
        .forEach(completionResultSet::addElement);
  }

  // Package-private to be used in anonymous Function
  static LookupElement createCompletionResult(PsiParameter parameter) {
    return LookupElementBuilder.create(
        new StringJoiner(" ")
            .add(parameter.getType().getPresentableText())
            .add(parameter.getName())
            .toString());
  }
}
