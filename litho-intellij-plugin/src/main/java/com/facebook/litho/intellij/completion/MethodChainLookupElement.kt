/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.intellij.completion

import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.google.common.annotations.VisibleForTesting
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

/** Completes suggestion with a chain of method calls. */
class MethodChainLookupElement
@VisibleForTesting
internal constructor(delegate: LookupElement, private val fromTemplate: Template) :
    LookupElementDecorator<LookupElement>(delegate) {

  override fun handleInsert(context: InsertionContext) {
    context.setAddCompletionChar(false)
    handleInsertInternal(
        context.editor,
        context.document,
        context.startOffset,
        context.tailOffset,
        context.project,
    )
    val data = HashMap<String, String>()
    data[EventLogger.KEY_TYPE] = "required_builder"
    data[EventLogger.KEY_TARGET] = EventLogger.VALUE_COMPLETION_TARGET_CALL
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data)
  }

  @VisibleForTesting
  internal fun handleInsertInternal(
      editor: Editor,
      document: Document,
      startOffset: Int,
      tailOffset: Int,
      project: Project,
  ) {
    // Copied from LiveTemplateLookupElementImpl
    val offsetAfterDot =
        document.getText(TextRange.create(startOffset, tailOffset)).indexOf('.') + 1 + startOffset
    document.deleteString(offsetAfterDot, tailOffset)
    TemplateManager.getInstance(project).startTemplate(editor, fromTemplate)
  }

  override fun renderElement(presentation: LookupElementPresentation) {
    super.renderElement(presentation)
    val tail = fromTemplate.templateText
    val secondMethodIndex = tail.indexOf('.')
    if (secondMethodIndex > 0) {
      val substring = tail.substring(secondMethodIndex).replace(Regex("[^A-Za-z0-9\$_.()]+"), "")
      presentation.appendTailText(substring, true)
    }
  }

  companion object {
    private const val TEMPLATE_INSERT_PLACEHOLDER = "insert_placeholder"
    private const val TEMPLATE_INSERT_PLACEHOLDER_C = "insert_placeholder_c"

    /**
     * @param lookupPresentation delegate for rendering the lookup element in completion suggestion
     *   results.
     * @param firstMethodName first method name in a method call chain. It is expected to be the
     *   same method as in the lookupPresentation.
     * @param otherMethodNames other names in a method call chain.
     * @param placeholder element in a PsiFile that will be replaced with the method call chain.
     */
    @JvmStatic
    fun create(
        lookupPresentation: LookupElement,
        firstMethodName: String,
        otherMethodNames: List<String>,
        placeholder: PsiElement,
        project: Project,
    ): LookupElement {
      val methodChain = createMethodChain(project, firstMethodName, otherMethodNames)
      val replaced = placeholder.replace(methodChain) ?: return lookupPresentation
      val reformatted =
          CodeStyleManager.getInstance(project).reformat(replaced, false)
              ?: return lookupPresentation
      val template = createTemplate(reformatted) ?: return lookupPresentation
      val prioritized =
          PrioritizedLookupElement.withPriority(lookupPresentation, Integer.MAX_VALUE.toDouble())
      return MethodChainLookupElement(prioritized, template)
    }

    @JvmStatic
    @VisibleForTesting
    internal fun createMethodChain(
        project: Project,
        firstName: String,
        methodNames: List<String>,
    ): PsiElement {
      val factory = JavaPsiFacade.getElementFactory(project)
      val expressionText = StringBuilder("$firstName($TEMPLATE_INSERT_PLACEHOLDER_C)")
      for (methodName in methodNames) {
        expressionText
            .append("\n.")
            .append(methodName)
            .append("(")
            .append(TEMPLATE_INSERT_PLACEHOLDER)
            .append(")")
      }
      return factory.createExpressionFromText(expressionText.toString(), null)
    }

    private fun createTemplate(from: PsiElement): Template? {
      val templateBuilder = TemplateBuilderImpl(from)
      PsiTreeUtil.processElements(from) { psiElement ->
        if (psiElement is PsiIdentifier) {
          val identifier = psiElement.getText()
          if (TEMPLATE_INSERT_PLACEHOLDER == identifier) {
            templateBuilder.replaceElement(psiElement, TextExpression(""))
          } else if (TEMPLATE_INSERT_PLACEHOLDER_C == identifier) {
            templateBuilder.replaceElement(psiElement, TextExpression("c"))
          }
        }
        true
      }
      return templateBuilder.buildTemplate()
    }
  }
}
