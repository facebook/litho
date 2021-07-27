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

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FromParameterProvider extends CompletionProvider<CompletionParameters> {
  private final String allowedMethodAnnotation;
  private final String parameterAnnotation;

  public FromParameterProvider(String allowedMethodAnnotation, String parameterAnnotation) {
    this.allowedMethodAnnotation = allowedMethodAnnotation;
    this.parameterAnnotation = parameterAnnotation;
  }

  @Override
  protected void addCompletions(
      CompletionParameters completionParameters,
      ProcessingContext processingContext,
      CompletionResultSet completionResultSet) {
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

    final PsiMethod psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    if (psiMethod == null) {
      return;
    }

    PsiAnnotation annotation =
        Arrays.stream(psiMethod.getAnnotations())
            .filter(
                methodAnnotation ->
                    allowedMethodAnnotation.contains(methodAnnotation.getQualifiedName()))
            .findAny()
            .orElse(null);
    if (annotation == null) {
      return;
    }

    final PsiAnnotationParameterList annotationParamList = annotation.getParameterList();
    if (annotationParamList == null) {
      return;
    }

    final PsiJavaCodeReferenceElement eventClass =
        PsiTreeUtil.findChildOfType(annotationParamList, PsiJavaCodeReferenceElement.class);
    // Annotation `Event` class is expected so that its fields can be extracted.
    if (eventClass == null) {
      return;
    }

    final PsiReference eventReferenceElement = eventClass.getReference();
    if (eventReferenceElement == null) {
      return;
    }
    final PsiElement eventReference = eventReferenceElement.resolve();
    if (!(eventReference instanceof PsiClass)) {
      return;
    }

    final List<PsiField> eventFields = Arrays.asList(((PsiClass) eventReference).getFields());

    eventFields.stream()
        .map(field -> createLookupElement(field))
        .map(
            lookupElement ->
                PrioritizedLookupElement.withPriority(lookupElement, Integer.MAX_VALUE))
        .forEach(completionResultSet::addElement);
  }

  private LookupElement createLookupElement(PsiField eventField) {
    String fieldType = eventField.getType().getPresentableText();
    String fieldName = eventField.getName();
    return LookupElementBuilder.create(parameterAnnotation + " " + fieldType + " " + fieldName)
        .withLookupStrings(Arrays.asList(fieldType, fieldName))
        .withPresentableText("@" + parameterAnnotation + " " + fieldType + " " + fieldName)
        .withInsertHandler(
            (context, elem) -> {
              final Map<String, String> data = new HashMap<>();
              data.put(EventLogger.KEY_TARGET, EventLogger.VALUE_COMPLETION_TARGET_ARGUMENT);
              data.put(EventLogger.KEY_CLASS, parameterAnnotation);
              LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data);
            });
  }
}
