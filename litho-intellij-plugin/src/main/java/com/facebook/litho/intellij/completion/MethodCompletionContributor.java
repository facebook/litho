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

import static com.facebook.litho.intellij.completion.CompletionUtils.METHOD_ANNOTATION;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.intellij.services.TemplateService;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class MethodCompletionContributor extends CompletionContributor {

  public MethodCompletionContributor() {
    extend(CompletionType.BASIC, METHOD_ANNOTATION, OnEventCompletionProvider.INSTANCE);
    extend(CompletionType.BASIC, METHOD_ANNOTATION, LayoutSpecMethodAnnotationsProvider.INSTANCE);
    extend(CompletionType.BASIC, METHOD_ANNOTATION, MountSpecMethodAnnotationsProvider.INSTANCE);
    extend(CompletionType.BASIC, METHOD_ANNOTATION, SectionSpecMethodAnnotationsProvider.INSTANCE);
  }

  static void addMethodCompletions(
      PsiElement position,
      Predicate<PsiClass> condition,
      Collection<String> annotationFQNs,
      CompletionResultSet result) {
    Optional<PsiClass> maybeCls = CompletionUtils.findFirstParent(position, condition);
    if (!maybeCls.isPresent()) {
      return;
    }

    final Project project = position.getProject();
    for (String annotationFQN : annotationFQNs) {
      LookupElement lookup =
          PrioritizedLookupElement.withPriority(
              createMethodLookup(annotationFQN, project, maybeCls.get()), Integer.MAX_VALUE);
      result.addElement(lookup);
    }
  }

  private static LookupElement createMethodLookup(
      String annotationFQN, Project project, PsiClass specClass) {
    final String annotation = StringUtil.getShortName(annotationFQN);
    final TemplateService templateService =
        ServiceManager.getService(project, TemplateService.class);
    final PsiMethod psiMethod = templateService.getMethodTemplate(annotation, project);
    if (psiMethod != null) {
      return createMethodLookup(
          psiMethod,
          Collections.singletonList(psiMethod),
          specClass,
          getOrCreateClass(annotationFQN, project),
          "@" + annotation,
          () -> log(annotation));
    }
    return SpecLookupElement.create(annotationFQN, project, (context, t) -> log(annotation));
  }

  static PsiClass getOrCreateClass(String qualifiedClassName, Project project) {
    PsiClass cls = PsiSearchUtils.findClass(project, qualifiedClassName);
    if (cls == null) {
      cls =
          JavaPsiFacade.getElementFactory(project)
              .createClass(LithoClassNames.shortName(qualifiedClassName));
    }
    return cls;
  }

  static LookupElementBuilder createMethodLookup(
      PsiMethod mainMethod,
      List<PsiMethod> methods,
      PsiClass specClass,
      PsiClass documentationCls,
      String lookupString,
      Runnable postProcessor) {
    return LookupElementBuilder.createWithIcon(mainMethod)
        .withPresentableText(lookupString)
        .withLookupString(lookupString)
        .withCaseSensitivity(false)
        .withInsertHandler(
            (context, item) -> {
              handleInsert(methods, context, specClass);
              postProcessor.run();
            })
        .appendTailText(" {...}", true)
        .withTypeText("Litho", true)
        .withPsiElement(documentationCls);
  }

  private static void log(String annotation) {
    final Map<String, String> data = new HashMap<>();
    data.put(EventLogger.KEY_TARGET, EventLogger.VALUE_COMPLETION_TARGET_METHOD);
    data.put(EventLogger.KEY_CLASS, annotation);
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data);
  }

  private static void handleInsert(
      List<PsiMethod> methods, InsertionContext insertionContext, PsiClass specClass) {
    // Remove lookup string. As in the JavaGenerateMemberCompletionContributor
    insertionContext
        .getDocument()
        .deleteString(insertionContext.getStartOffset() - 1, insertionContext.getTailOffset());
    insertionContext.commitDocument();

    new MethodGenerateHandler(
            methods, specClass, insertionContext.getDocument(), insertionContext.getProject())
        .invoke(
            insertionContext.getProject(),
            insertionContext.getEditor(),
            insertionContext.getFile());
  }
}
