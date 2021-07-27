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

import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/** Offers completion for the {@code @Param} method parameters in the Litho Spec class. */
public class ParamCompletionContributor extends CompletionContributor {
  private static final Set<String> ALLOWED_METHOD_ANNOTATIONS = new HashSet<>();

  static {
    ALLOWED_METHOD_ANNOTATIONS.add(OnEvent.class.getTypeName());
    ALLOWED_METHOD_ANNOTATIONS.add(OnUpdateState.class.getTypeName());
    ALLOWED_METHOD_ANNOTATIONS.add(OnUpdateStateWithTransition.class.getTypeName());
  }

  public ParamCompletionContributor() {
    extend(CompletionType.BASIC, METHOD_PARAMETER_ANNOTATION, FromEventParameterProvider.INSTANCE);
    extend(
        CompletionType.BASIC, METHOD_PARAMETER_ANNOTATION, FromTriggerParameterProvider.INSTANCE);
    extend(CompletionType.BASIC, METHOD_PARAMETER_ANNOTATION, typeCompletionProvider());
  }

  private static CompletionProvider<CompletionParameters> typeCompletionProvider() {
    return new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(
          @Nullable CompletionParameters completionParameters,
          ProcessingContext processingContext,
          @Nullable CompletionResultSet completionResultSet) {
        if (completionResultSet == null) {
          return;
        }
        if (completionParameters == null) {
          return;
        }
        final PsiElement element = completionParameters.getPosition();

        final PsiMethod containingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (containingMethod == null) {
          return;
        }
        final boolean isAllowedAnnotation =
            Arrays.stream(containingMethod.getAnnotations())
                .map(methodAnnotation -> methodAnnotation.getQualifiedName())
                .anyMatch(ALLOWED_METHOD_ANNOTATIONS::contains);
        if (!isAllowedAnnotation) {
          return;
        }

        final PsiClass cls = containingMethod.getContainingClass();
        if (!LithoPluginUtils.isLithoSpec(cls)) {
          return;
        }

        Collection<String> replacedQualifiedNames =
            Collections.singleton(Param.class.getTypeName());
        InsertHandler<LookupElement> insertHandler =
            (insertionContext, item) -> {
              final Map<String, String> data = new HashMap<>();
              data.put(EventLogger.KEY_TARGET, EventLogger.VALUE_COMPLETION_TARGET_PARAMETER);
              data.put(EventLogger.KEY_CLASS, item.getLookupString());
              LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data);
            };

        QualifiedNamesFilter qualifiedNamesFilter =
            new QualifiedNamesFilter(replacedQualifiedNames, insertHandler);

        final ReplacingConsumer replacingConsumer =
            new ReplacingConsumer(completionResultSet, qualifiedNamesFilter, false);

        qualifiedNamesFilter.addRemainingCompletions(
            completionParameters.getPosition().getProject(), completionResultSet);
        completionResultSet.runRemainingContributors(completionParameters, replacingConsumer);
      }
    };
  }
}
