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

package com.facebook.litho.intellij.inspections

import com.facebook.litho.annotations.PropSetter
import com.facebook.litho.intellij.IntervalLogger
import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.services.ComponentGenerateService
import com.facebook.litho.specmodels.model.SpecModelValidationError
import com.facebook.litho.specmodels.processor.PsiAnnotationProxyUtils
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil

/** Annotator creates quick fix to add existing EventHandler to the method without arguments. */
class MethodCallAnnotator : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    DEBUG_LOGGER.logStep("start $element")
    // Implementation similar to {@link HighlightMethodUtil#checkMethodCall}
    if (element !is PsiMethodCallExpression) {
      return
    }

    // MethodCall == method usage
    formatRequiredProp(element, holder)

    addEventHandlerFix(holder, element)

    DEBUG_LOGGER.logStep("end $element")
  }

  companion object {
    private val DEBUG_LOGGER: IntervalLogger =
        IntervalLogger(Logger.getInstance(MethodCallAnnotator::class.java))

    const val FIX_FAMILY_NAME: String = "LithoFix"

    private fun addEventHandlerFix(holder: AnnotationHolder, methodCall: PsiMethodCallExpression) {
      val list = methodCall.argumentList
      if (!list.isEmpty) {
        return
      }
      val eventQualifiedName = LithoPluginUtils.resolveEventName(methodCall) ?: return
      val parentCls =
          PsiTreeUtil.findFirstParent(methodCall) { it is PsiClass } as? PsiClass ?: return
      val parentModel =
          ComponentGenerateService.getInstance().getOrCreateSpecModel(parentCls) ?: return
      val implementedEventHandlers = parentModel.eventMethods
      val componentQualifiedName = parentModel.componentTypeName.toString()
      val elementFactory = JavaPsiFacade.getInstance(methodCall.project).elementFactory
      val message = "Add ${AddArgumentFix.getCapitalizedMethoName(methodCall)}"
      val error = SpecModelValidationError(list, message)

      val fixes: List<IntentionAction> =
          implementedEventHandlers
              .asSequence()
              .filter { handler -> eventQualifiedName == handler.typeModel?.reflectionName }
              .map { handler -> handler.name.toString() }
              .distinct()
              .map { methodName ->
                AddArgumentFix.createAddMethodCallFix(
                    methodCall,
                    componentQualifiedName,
                    methodName,
                    elementFactory,
                )
              }
              .toList()

      AnnotatorUtils.addError(holder, error, fixes)
    }

    private fun formatRequiredProp(
        expression: PsiMethodCallExpression,
        holder: AnnotationHolder,
    ) {
      val method = expression.resolveMethod() ?: return
      @Suppress("UNCHECKED_CAST")
      val annotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(
              method,
              PropSetter::class.java as Class<java.lang.annotation.Annotation>,
          ) as? PropSetter ?: return
      if (!annotation.required) {
        return
      }
      if (expression.methodExpression.referenceNameElement == null) {
        return
      }
      holder
          .newSilentAnnotation(HighlightSeverity.INFORMATION)
          .textAttributes(DefaultLanguageHighlighterColors.KEYWORD)
          .create()
    }
  }
}
