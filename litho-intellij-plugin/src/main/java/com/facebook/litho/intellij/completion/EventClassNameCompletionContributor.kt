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

import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.completion.CompletionUtils.METHOD_ANNOTATION_PARAMETER
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class EventClassNameCompletionContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, METHOD_ANNOTATION_PARAMETER, typeCompletionProvider())
  }

  internal class EventClassFilter(
      private val handler: InsertHandler<LookupElement>,
  ) : LookupElementFilter {

    override fun apply(lookupElement: LookupElement, element: PsiClass): LookupElement? {
      if (!LithoPluginUtils.isEvent(element)) {
        return null
      }
      val name = element.name ?: return null
      return createLookupElement(name, handler)
    }

    override fun addRemainingCompletions(project: Project, result: CompletionResultSet): Unit = Unit
  }

  companion object {
    private val ALLOWED_ANNOTATIONS: Set<String> = hashSetOf(
        OnEvent::class.java.typeName,
        OnTrigger::class.java.typeName,
    )

    private fun typeCompletionProvider(): CompletionProvider<CompletionParameters> {
      return object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            completionParameters: CompletionParameters,
            processingContext: ProcessingContext,
            completionResultSet: CompletionResultSet,
        ) {
          if (!LithoPluginUtils.isLithoSpec(completionParameters.originalFile)) {
            return
          }

          val element = completionParameters.position

          val annotatedMethod =
              PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return

          val annotation =
              annotatedMethod.annotations
                  .asSequence()
                  .mapNotNull { it.qualifiedName }
                  .firstOrNull { ALLOWED_ANNOTATIONS.contains(it) } ?: return

          val returnType = annotatedMethod.returnType
          if (returnType == null || returnType.presentableText == "void") {
            return
          }

          val insertHandler =
              InsertHandler<LookupElement> { _, _ ->
                val data = HashMap<String, String>()
                data[EventLogger.KEY_TARGET] = EventLogger.VALUE_COMPLETION_TARGET_PARAMETER
                data[EventLogger.KEY_CLASS] = annotation
                LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data)
              }

          val eventClassFilter = EventClassFilter(insertHandler)

          val replacingConsumer = ReplacingConsumer(completionResultSet, eventClassFilter, true)

          completionResultSet.runRemainingContributors(completionParameters, replacingConsumer)
        }
      }
    }

    private fun createLookupElement(
        qualifiedName: String,
        handler: InsertHandler<LookupElement>,
    ): LookupElement {
      return LookupElementBuilder.create("$qualifiedName.class")
          .withTypeText("Litho Event")
          .withInsertHandler(handler)
    }
  }
}
