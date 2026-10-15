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
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class FromParameterProvider(
    private val allowedMethodAnnotation: String,
    private val parameterAnnotation: String,
) : CompletionProvider<CompletionParameters>() {

  override fun addCompletions(
      completionParameters: CompletionParameters,
      processingContext: ProcessingContext,
      completionResultSet: CompletionResultSet,
  ) {
    if (!LithoPluginUtils.isLithoSpec(completionParameters.originalFile)) {
      return
    }

    val element = completionParameters.position

    val psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return

    val annotation =
        psiMethod.annotations.firstOrNull { methodAnnotation ->
          methodAnnotation.qualifiedName?.let { allowedMethodAnnotation.contains(it) } == true
        } ?: return

    val annotationParamList = annotation.parameterList

    val eventClass =
        PsiTreeUtil.findChildOfType(annotationParamList, PsiJavaCodeReferenceElement::class.java)
            ?: return

    val eventReferenceElement = eventClass.reference ?: return
    val eventReference = eventReferenceElement.resolve()
    if (eventReference !is PsiClass) {
      return
    }

    val eventFields = eventReference.fields.toList()

    for (field in eventFields) {
      val lookupElement = createLookupElement(field)
      val prioritized =
          PrioritizedLookupElement.withPriority(lookupElement, Integer.MAX_VALUE.toDouble())
      completionResultSet.addElement(prioritized)
    }
  }

  private fun createLookupElement(eventField: PsiField): LookupElement {
    val fieldType = eventField.type.presentableText
    val fieldName = eventField.name
    return LookupElementBuilder.create("$parameterAnnotation $fieldType $fieldName")
        .withLookupStrings(listOf(fieldType, fieldName))
        .withPresentableText("@$parameterAnnotation $fieldType $fieldName")
        .withInsertHandler { _, _ ->
          val data = HashMap<String, String>()
          data[EventLogger.KEY_TARGET] = EventLogger.VALUE_COMPLETION_TARGET_ARGUMENT
          data[EventLogger.KEY_CLASS] = parameterAnnotation
          LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data)
        }
  }
}
