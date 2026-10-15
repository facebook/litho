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

import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.facebook.litho.intellij.services.ComponentGenerateService
import com.facebook.litho.specmodels.model.EventDeclarationModel
import com.facebook.litho.specmodels.model.EventMethod
import com.facebook.litho.specmodels.model.SpecMethodModel
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.java.JavaLanguage
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

/** Offers completion for the `@EventHandler` method parameters in the Litho Spec class. */
class EventHandlerCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, codeReferencePattern(), typeCompletionProvider())
  }

  companion object {
    private fun codeReferencePattern(): ElementPattern<out PsiElement> {
      return PlatformPatterns.psiElement(PsiIdentifier::class.java)
          .withParent(PsiJavaCodeReferenceElement::class.java)
          .withLanguage(JavaLanguage.INSTANCE)
    }

    private fun typeCompletionProvider(): CompletionProvider<CompletionParameters> {
      return object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            completionParameters: CompletionParameters,
            processingContext: ProcessingContext,
            completionResultSet: CompletionResultSet,
        ) {
          val element = completionParameters.position

          val eventHandlerSetter =
              PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression::class.java) ?: return
          val eventQualifiedName = LithoPluginUtils.resolveEventName(eventHandlerSetter) ?: return
          val parentCls =
              PsiTreeUtil.findFirstParent(eventHandlerSetter) { it is PsiClass } as? PsiClass
                  ?: return
          val parentModel =
              ComponentGenerateService.getInstance().getOrCreateSpecModel(parentCls) ?: return
          val implementedEventHandlers = parentModel.eventMethods

          implementedEventHandlers
              .asSequence()
              .filter { handler -> eventQualifiedName == handler.typeModel?.reflectionName }
              .map { handler ->
                createLookupElement(
                    eventHandlerSetter.methodExpression.referenceName,
                    parentModel.componentName,
                    handler,
                )
              }
              .map { lookupElement ->
                PrioritizedLookupElement.withPriority(lookupElement, Int.MAX_VALUE.toDouble())
              }
              .forEach { completionResultSet.addElement(it) }
        }
      }
    }

    private fun createLookupElement(
        methodCallName: String?,
        componentName: String,
        handler: SpecMethodModel<EventMethod, EventDeclarationModel>,
    ): LookupElement {
      val methodName = handler.name.toString()
      return LookupElementBuilder.create("$componentName.$methodName()")
          .withLookupStrings(listOfNotNull(methodName, methodCallName))
          .withPresentableText(methodName)
          .withTypeText("EventHandler<" + (handler.typeModel?.reflectionName ?: "") + ">")
          .withInsertHandler { context, _ ->
            context.editor.caretModel.moveCaretRelatively(-1, 0, false, false, false)
            log("EventHandler")
          }
    }

    private fun log(cls: String) {
      val data = HashMap<String, String>()
      data[EventLogger.KEY_TARGET] = EventLogger.VALUE_COMPLETION_TARGET_ARGUMENT
      data[EventLogger.KEY_CLASS] = cls
      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data)
    }
  }
}
