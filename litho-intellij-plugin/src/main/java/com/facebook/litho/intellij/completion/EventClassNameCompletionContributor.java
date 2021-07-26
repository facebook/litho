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

import static com.facebook.litho.intellij.completion.CompletionUtils.METHOD_ANNOTATION_PARAMETER;

import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnTrigger;
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
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class EventClassNameCompletionContributor extends CompletionContributor {
  private static final Set<String> ALLOWED_ANNOTATIONS = new HashSet<>();

  static class EventClassFilter implements LookupElementFilter {
    private InsertHandler<LookupElement> handler;

    public EventClassFilter(InsertHandler<LookupElement> handler) {
      this.handler = handler;
    }

    @Override
    public LookupElement apply(LookupElement lookupElement, PsiClass element) {
      return Optional.ofNullable(element)
          .filter(LithoPluginUtils::isEvent)
          .map(PsiClass::getName)
          .map(qualifiedName -> createLookupElement(qualifiedName, handler))
          .orElse(null);
    }

    @Override
    public void addRemainingCompletions(Project project, CompletionResultSet result) {}
  }

  static {
    ALLOWED_ANNOTATIONS.add(OnEvent.class.getTypeName());
    ALLOWED_ANNOTATIONS.add(OnTrigger.class.getTypeName());
  }

  public EventClassNameCompletionContributor() {
    extend(CompletionType.BASIC, METHOD_ANNOTATION_PARAMETER, typeCompletionProvider());
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

        if (!LithoPluginUtils.isLithoSpec(completionParameters.getOriginalFile())) {
          return;
        }

        final PsiElement element = completionParameters.getPosition();

        final PsiMethod annotatedMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (annotatedMethod == null) {
          return;
        }

        String annotation =
            Arrays.stream(annotatedMethod.getAnnotations())
                .map(methodAnnotation -> methodAnnotation.getQualifiedName())
                .filter(ALLOWED_ANNOTATIONS::contains)
                .findAny()
                .orElse(null);
        if (annotation == null) {
          return;
        }

        final PsiType returnType = annotatedMethod.getReturnType();
        if (returnType == null || returnType.getPresentableText().equals("void")) {
          return;
        }

        InsertHandler<LookupElement> insertHandler =
            (insertionContext, item) -> {
              final Map<String, String> data = new HashMap<>();
              data.put(EventLogger.KEY_TARGET, EventLogger.VALUE_COMPLETION_TARGET_PARAMETER);
              data.put(EventLogger.KEY_CLASS, annotation);
              LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data);
            };

        EventClassFilter eventClassFilter = new EventClassFilter(insertHandler);

        final ReplacingConsumer replacingConsumer =
            new ReplacingConsumer(completionResultSet, eventClassFilter, true);

        completionResultSet.runRemainingContributors(completionParameters, replacingConsumer);
      }
    };
  }

  private static LookupElement createLookupElement(
      String qualifiedName, InsertHandler<LookupElement> handler) {
    return LookupElementBuilder.create(qualifiedName + ".class")
        .withTypeText("Litho Event")
        .withInsertHandler(handler);
  }
}
