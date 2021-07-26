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

import static com.facebook.litho.intellij.completion.CompletionUtils.METHOD_PARAMETER_ANNOTATION;

import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.InjectProp;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.specmodels.model.DelegateMethodDescription;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class LayoutSpecMethodParameterAnnotationsContributor extends CompletionContributor {

  public LayoutSpecMethodParameterAnnotationsContributor() {
    extend(CompletionType.BASIC, METHOD_PARAMETER_ANNOTATION, Provider.INSTANCE);
  }

  static class Provider extends CompletionProvider<CompletionParameters> {
    static final CompletionProvider<CompletionParameters> INSTANCE = new Provider();

    @VisibleForTesting
    static final Map<String, Set<String>> LAYOUT_SPEC_DELEGATE_METHOD_TO_PARAMETER_ANNOTATIONS;

    static {
      LAYOUT_SPEC_DELEGATE_METHOD_TO_PARAMETER_ANNOTATIONS =
          DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP.entrySet().stream()
              .collect(
                  Collectors.toMap(
                      e -> e.getKey().getTypeName(),
                      e -> {
                        final DelegateMethodDescription methodDescription = e.getValue();
                        return Stream.concat(
                                methodDescription.interStageInputAnnotations.stream(),
                                methodDescription.optionalParameterTypes.stream()
                                    .map(Provider::getTypeAnnotation)
                                    .filter(Objects::nonNull))
                            .map(Class::getTypeName)
                            .collect(Collectors.toSet());
                      }));
    }

    @Nullable
    private static Class<? extends Annotation> getTypeAnnotation(
        DelegateMethodDescription.OptionalParameterType type) {
      switch (type) {
        case PROP:
          return Prop.class;
        case STATE:
          return State.class;
        case TREE_PROP:
          return TreeProp.class;
        case INJECT_PROP:
          return InjectProp.class;
        case PARAM:
          return Param.class;
        case CACHED_VALUE:
          return CachedValue.class;
        default:
          return null;
      }
    }

    @Override
    protected void addCompletions(
        CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
      PsiElement element = parameters.getPosition();
      InsertHandler<LookupElement> insertHandler =
          (insertionContext, item) -> {
            final Map<String, String> data = new HashMap<>();
            data.put(EventLogger.KEY_TARGET, EventLogger.VALUE_COMPLETION_TARGET_PARAMETER);
            data.put(EventLogger.KEY_CLASS, item.getLookupString());
            LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data);
          };
      Optional.ofNullable(PsiTreeUtil.findFirstParent(element, PsiMethod.class::isInstance))
          .flatMap(method -> getParameterAnnotations((PsiMethod) method))
          .map(
              annotations ->
                  new ReplacingConsumer(
                      result, new QualifiedNamesFilter(annotations, insertHandler), false))
          .ifPresent(
              replacingConsumer -> {
                // We want our suggestions at the top, that's why adding them first
                replacingConsumer.filterElement.addRemainingCompletions(
                    parameters.getPosition().getProject(), result);
                result.runRemainingContributors(parameters, replacingConsumer);
              });
    }

    private static Optional<Set<String>> getParameterAnnotations(PsiMethod method) {
      return Arrays.stream(method.getAnnotations())
          .map(PsiAnnotation::getQualifiedName)
          .map(LAYOUT_SPEC_DELEGATE_METHOD_TO_PARAMETER_ANNOTATIONS::get)
          .filter(Objects::nonNull)
          .findAny();
    }
  }
}
