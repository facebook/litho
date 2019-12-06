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

import com.facebook.litho.annotations.RequiredProp;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.specmodels.processor.PsiAnnotationProxyUtils;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ProcessingContext;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contributor improves available method completions: prioritizes existing required prop setters,
 * adds new component builder completion.
 */
public class RequiredPropMethodContributor extends CompletionContributor {

  public RequiredPropMethodContributor() {
    extend(CompletionType.BASIC, CompletionUtils.AFTER_DOT, RequiredPropMethodProvider.INSTANCE);
  }

  public static class RequiredPropMethodProvider extends CompletionProvider<CompletionParameters> {
    static final CompletionProvider<CompletionParameters> INSTANCE =
        new RequiredPropMethodProvider();

    @Override
    protected void addCompletions(
        CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
      result.runRemainingContributors(
          parameters,
          suggestedCompletion -> {
            LookupElement suggestedLookup = suggestedCompletion.getLookupElement();
            PsiElement suggestedElement = suggestedLookup.getPsiElement();
            CompletionResult replacingCompletion = null;

            if (suggestedElement instanceof PsiMethod) {
              PsiMethod suggestedMethod = (PsiMethod) suggestedElement;

              RequiredProp requiredPropAnnotation =
                  PsiAnnotationProxyUtils.findAnnotationInHierarchy(
                      suggestedMethod, RequiredProp.class);
              if (requiredPropAnnotation != null) {
                RequiredPropLookupElement newLookupElement =
                    RequiredPropLookupElement.create(
                        suggestedLookup,
                        isMainRequiredPropertySetter(
                            suggestedMethod.getName(), requiredPropAnnotation));
                replacingCompletion = CompletionUtils.wrap(suggestedCompletion, newLookupElement);

              } else if (isComponentCreateMethod(suggestedMethod)) {
                Optional.of(suggestedMethod.getParent())
                    .map(cls -> findRequiredPropSetterNames((PsiClass) cls))
                    .filter(methodNames -> !methodNames.isEmpty())
                    .map(
                        methodNames ->
                            MethodChainLookupElement.create(
                                suggestedLookup,
                                suggestedMethod.getName(),
                                methodNames,
                                parameters.getPosition().getPrevSibling(),
                                parameters.getEditor().getProject()))
                    .map(
                        newLookupElement ->
                            CompletionUtils.wrap(suggestedCompletion, newLookupElement))
                    .ifPresent(result::passResult);
              }
            }
            result.passResult(
                replacingCompletion == null ? suggestedCompletion : replacingCompletion);
          });
    }

    /**
     * Required property setter has different variations: {@code text()} (main), {@code textRes()},
     * {@code textAttr()}, etc.
     */
    private static boolean isMainRequiredPropertySetter(
        String methodName, RequiredProp methodAnnotation) {
      return methodName.equals(methodAnnotation.value());
    }

    @VisibleForTesting
    static boolean isComponentCreateMethod(PsiMethod method) {
      if (!"create".equals(method.getName())) {
        return false;
      }
      if (method.getParameters().length != 1) {
        return false;
      }
      PsiElement parent = method.getParent();
      if (!(parent instanceof PsiClass)) {
        return false;
      }
      return LithoPluginUtils.isComponentClass((PsiClass) parent);
    }

    @VisibleForTesting
    static List<String> findRequiredPropSetterNames(PsiClass component) {
      return Optional.ofNullable(component.findInnerClassByName("Builder", false))
          .map(PsiClass::getMethods)
          .map(
              methods ->
                  Arrays.stream(methods)
                      .map(
                          method -> {
                            RequiredProp annotation =
                                PsiAnnotationProxyUtils.findAnnotationInHierarchy(
                                    method, RequiredProp.class);
                            if (annotation == null) {
                              return null;
                            }
                            String methodName = method.getName();
                            if (!isMainRequiredPropertySetter(methodName, annotation)) {
                              return null;
                            }
                            return methodName;
                          })
                      .filter(Objects::nonNull))
          .orElse(Stream.empty())
          .collect(Collectors.toList());
    }
  }
}
