/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.intellij.completion;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
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
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ProcessingContext;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

/** Contributor suggests completion for the Click event in the Litho Spec. */
public class OnEventCompletionContributor extends CompletionContributor {

  public OnEventCompletionContributor() {
    extend(CompletionType.BASIC, CompletionUtils.METHOD_ANNOTATION, typeCompletionProvider());
  }

  private static CompletionProvider<CompletionParameters> typeCompletionProvider() {
    return new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(
          @NotNull CompletionParameters parameters,
          ProcessingContext context,
          @NotNull CompletionResultSet result) {
        CompletionUtils.findFirstParent(parameters.getPosition(), LithoPluginUtils::isLithoSpec)
            .ifPresent(
                lithoSpecCls -> {
                  PsiClass clickEventCls =
                      getOrCreateClass(
                          lithoSpecCls.getProject(), LithoClassNames.CLICK_EVENT_CLASS_NAME);
                  result.addElement(
                      createMethodLookup(
                          OnEventGenerateUtils.createOnEventMethod(
                              lithoSpecCls, clickEventCls, Collections.emptyList()),
                          clickEventCls,
                          OnEventGenerateUtils.createOnEventLookupString(clickEventCls)));
                });
      }
    };
  }

  private static PsiClass getOrCreateClass(Project project, String qualifiedClassName) {
    PsiClass cls = PsiSearchUtils.findClass(project, qualifiedClassName);
    if (cls == null) {
      cls =
          JavaPsiFacade.getElementFactory(project)
              .createClass(LithoClassNames.shortName(qualifiedClassName));
    }
    return cls;
  }

  private static LookupElementBuilder createMethodLookup(
      PsiMethod method, PsiClass documentationCls, String lookupString) {
    return LookupElementBuilder.createWithIcon(method)
        .withPresentableText(lookupString)
        .withLookupString(lookupString)
        .withCaseSensitivity(false)
        .withInsertHandler(getOnEventInsertHandler(method))
        .appendTailText(" {...}", true)
        .withTypeText("Litho", true)
        .withPsiElement(documentationCls);
  }

  /** Creates handler to insert given method in the lookup element insertion context. */
  private static InsertHandler<LookupElement> getOnEventInsertHandler(PsiMethod method) {
    return (insertionContext, item) -> {
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

      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_ON_EVENT_COMPLETION);

      LithoPluginUtils.getFirstLayoutSpec(insertionContext.getFile())
          .ifPresent(ComponentGenerateUtils::updateLayoutComponent);
    };
  }
}
