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

import static com.facebook.litho.intellij.completion.MethodCompletionContributor.createMethodLookup;

import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.intellij.services.TemplateService;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ProcessingContext;
import java.util.HashSet;
import java.util.Set;

/**
 * Provider emphasized completion results for valid {@link
 * com.facebook.litho.annotations.LayoutSpec} method annotations.
 *
 * @see LayoutSpecModelFactory
 */
class LayoutSpecMethodAnnotationsProvider extends CompletionProvider<CompletionParameters> {
  static final CompletionProvider<CompletionParameters> INSTANCE =
      new LayoutSpecMethodAnnotationsProvider();

  static final Set<String> ANNOTATION_QUALIFIED_NAMES = new HashSet<>();

  static {
    for (Class permittedMethod : LayoutSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS) {
      ANNOTATION_QUALIFIED_NAMES.add(permittedMethod.getTypeName());
    }
    /*
    OnEvent is added via other mechanism
    ANNOTATION_QUALIFIED_NAMES.add(OnEvent.class.getTypeName());
    */
    ANNOTATION_QUALIFIED_NAMES.add(OnTrigger.class.getTypeName());
    ANNOTATION_QUALIFIED_NAMES.add(OnUpdateState.class.getTypeName());
    ANNOTATION_QUALIFIED_NAMES.add(OnUpdateStateWithTransition.class.getTypeName());
  }

  @Override
  protected void addCompletions(
      CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
    PsiElement position = parameters.getPosition();
    if (!CompletionUtils.findFirstParent(position, LithoPluginUtils::isLayoutSpec).isPresent())
      return;

    final Project project = position.getProject();
    for (String annotationFQN : ANNOTATION_QUALIFIED_NAMES) {
      LookupElement lookup =
          PrioritizedLookupElement.withPriority(
              createLookup(annotationFQN, project), Integer.MAX_VALUE);
      result.addElement(lookup);
    }
  }

  private static LookupElement createLookup(String annotationFQN, Project project) {
    final String annotation = StringUtil.getShortName(annotationFQN);
    final PsiMethod psiMethod =
        ServiceManager.getService(project, TemplateService.class)
            .getMethodTemplate(annotation, project);
    if (psiMethod != null) {
      PsiClass annotationCls = MethodCompletionContributor.getOrCreateClass(annotationFQN, project);
      return createMethodLookup(psiMethod, annotationCls, "@" + annotation, () -> log(annotation));
    }
    return SpecLookupElement.create(annotationFQN, project, (context1, item) -> log(annotation));
  }

  private static void log(String annotation) {
    LithoLoggerProvider.getEventLogger()
        .log(EventLogger.EVENT_COMPLETION_METHOD + "." + annotation);
  }
}
