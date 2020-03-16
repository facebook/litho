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

import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TextExpression;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.List;
import java.util.Optional;

/** Completes suggestion with a chain of method calls. */
public class MethodChainLookupElement extends LookupElementDecorator<LookupElement> {
  private static final String TEMPLATE_INSERT_PLACEHOLDER = "insert_placeholder";
  private static final String TEMPLATE_INSERT_PLACEHOLDER_C = "insert_placeholder_c";
  private final Template fromTemplate;

  /**
   * @param lookupPresentation delegate for rendering the lookup element in completion suggestion
   *     results.
   * @param firstMethodName first method name in a method call chain. It is expected to be the same
   *     method as in the lookupPresentation.
   * @param otherMethodNames other names in a method call chain.
   * @param placeholder element in a PsiFile that will be replaced with the method call chain.
   */
  public static LookupElement create(
      LookupElement lookupPresentation,
      String firstMethodName,
      List<? extends String> otherMethodNames,
      PsiElement placeholder,
      Project project) {
    return Optional.of(createMethodChain(project, firstMethodName, otherMethodNames))
        .map(placeholder::replace)
        .map(elementInTree -> CodeStyleManager.getInstance(project).reformat(elementInTree, false))
        .map(MethodChainLookupElement::createTemplate)
        .<LookupElement>map(
            template -> {
              final LookupElement prioritized =
                  PrioritizedLookupElement.withPriority(lookupPresentation, Integer.MAX_VALUE);
              return new MethodChainLookupElement(prioritized, template);
            })
        .orElse(lookupPresentation);
  }

  @VisibleForTesting
  static PsiExpression createMethodChain(
      Project project, String firstName, List<? extends String> methodNames) {
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
    StringBuilder expressionText =
        new StringBuilder(firstName + "(" + TEMPLATE_INSERT_PLACEHOLDER_C + ")");
    methodNames.forEach(
        methodName ->
            expressionText
                .append("\n.")
                .append(methodName)
                .append("(")
                .append(TEMPLATE_INSERT_PLACEHOLDER)
                .append(")"));
    return factory.createExpressionFromText(expressionText.toString(), null);
  }

  private static Template createTemplate(PsiElement from) {
    TemplateBuilderImpl templateBuilder = new TemplateBuilderImpl(from);
    PsiTreeUtil.processElements(
        from,
        psiElement -> {
          if (psiElement instanceof PsiIdentifier) {
            PsiIdentifier psiIdentifier = (PsiIdentifier) psiElement;
            String identifier = psiIdentifier.getText();
            if (TEMPLATE_INSERT_PLACEHOLDER.equals(identifier)) {
              templateBuilder.replaceElement(psiIdentifier, new TextExpression(""));
            } else if (TEMPLATE_INSERT_PLACEHOLDER_C.equals(identifier)) {
              templateBuilder.replaceElement(psiIdentifier, new TextExpression("c"));
            }
          }
          return true;
        });
    Template template = templateBuilder.buildTemplate();
    template.setToReformat(true);
    return template;
  }

  @VisibleForTesting
  MethodChainLookupElement(LookupElement delegate, Template fromTemplate) {
    super(delegate);
    this.fromTemplate = fromTemplate;
  }

  @Override
  public void handleInsert(InsertionContext context) {
    // Copied from LiveTemplateLookupElementImpl
    context.getDocument().deleteString(findOffsetAfterDot(context), context.getTailOffset());
    context.setAddCompletionChar(false);
    TemplateManager.getInstance(context.getProject())
        .startTemplate(context.getEditor(), fromTemplate);
    LithoLoggerProvider.getEventLogger()
        .log(EventLogger.EVENT_COMPLETION_REQUIRED_PROP + ".builder");
  }

  private static int findOffsetAfterDot(InsertionContext context) {
    final int startOffset = context.getStartOffset();
    return context
            .getDocument()
            .getText(TextRange.create(startOffset, context.getTailOffset()))
            .indexOf('.')
        + 1
        + startOffset;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    super.renderElement(presentation);
    String tail = fromTemplate.getTemplateText();
    int secondMethodIndex = tail.indexOf('.');
    if (secondMethodIndex > 0) {
      String substring = tail.substring(secondMethodIndex).replaceAll("[^A-Za-z0-9$_.()]+", "");
      presentation.appendTailText(substring, true);
    }
  }
}
