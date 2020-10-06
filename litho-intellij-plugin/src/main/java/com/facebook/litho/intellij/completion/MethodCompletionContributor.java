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
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

public class MethodCompletionContributor extends CompletionContributor {

  public MethodCompletionContributor() {
    extend(CompletionType.BASIC, METHOD_ANNOTATION, OnEventCompletionProvider.INSTANCE);
    extend(CompletionType.BASIC, METHOD_ANNOTATION, LayoutSpecMethodAnnotationsProvider.INSTANCE);
    extend(CompletionType.BASIC, METHOD_ANNOTATION, MountSpecMethodAnnotationsProvider.INSTANCE);
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
      PsiMethod method, PsiClass documentationCls, String lookupString, Runnable postProcessor) {
    return LookupElementBuilder.createWithIcon(method)
        .withPresentableText(lookupString)
        .withLookupString(lookupString)
        .withCaseSensitivity(false)
        .withInsertHandler(
            (context, item) -> {
              handleInsert(method, context);
              postProcessor.run();
            })
        .appendTailText(" {...}", true)
        .withTypeText("Litho", true)
        .withPsiElement(documentationCls);
  }

  private static void handleInsert(PsiMethod method, InsertionContext insertionContext) {
    // Remove lookup string. As in the JavaGenerateMemberCompletionContributor
    insertionContext
        .getDocument()
        .deleteString(insertionContext.getStartOffset() - 1, insertionContext.getTailOffset());
    insertionContext.commitDocument();

    // Insert generation infos
    new MethodGenerateHandler(method)
        .invoke(
            insertionContext.getProject(),
            insertionContext.getEditor(),
            insertionContext.getFile());
  }
}
