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

import com.facebook.litho.annotations.RequiredProp
import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.specmodels.processor.PsiAnnotationProxyUtils
import com.google.common.annotations.VisibleForTesting
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResult
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.util.ProcessingContext
import java.util.Optional

/**
 * Contributor improves available method completions: prioritizes existing required prop setters,
 * adds new component builder completion.
 */
class RequiredPropMethodContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, CompletionUtils.AFTER_DOT, RequiredPropMethodProvider)
  }

  object RequiredPropMethodProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
      result.runRemainingContributors(parameters) { suggestedCompletion ->
        val suggestedLookup = suggestedCompletion.lookupElement
        val suggestedElement = suggestedLookup.psiElement
        var replacingCompletion: CompletionResult? = null

        if (suggestedElement is PsiMethod) {
          @Suppress("UNCHECKED_CAST")
          val requiredPropAnnotation =
              PsiAnnotationProxyUtils.findAnnotationInHierarchy(
                  suggestedElement as PsiModifierListOwner,
                  RequiredProp::class.java as Class<java.lang.annotation.Annotation>,
              ) as? RequiredProp
          if (requiredPropAnnotation != null) {
            val newLookupElement =
                RequiredPropLookupElement.create(
                    suggestedLookup,
                    isMainRequiredPropertySetter(suggestedElement.name, requiredPropAnnotation),
                )
            replacingCompletion = CompletionUtils.wrap(suggestedCompletion, newLookupElement)
          } else if (isComponentCreateMethod(suggestedElement)) {
            createMethodLookup(
                suggestedElement,
                suggestedLookup,
                parameters.position.prevSibling,
                parameters.editor.project,
            )
                .map { newLookupElement ->
                  CompletionUtils.wrap(suggestedCompletion, newLookupElement)
                }
                .ifPresent { result.passResult(it) }
          }
        }
        result.passResult(replacingCompletion ?: suggestedCompletion)
      }
    }

    @JvmStatic
    @VisibleForTesting
    internal fun createMethodLookup(
        createMethod: PsiMethod,
        delegate: LookupElement,
        placeholder: PsiElement,
        project: Project?,
    ): Optional<LookupElement> {
      if (project == null) {
        return Optional.empty()
      }
      val parent = createMethod.parent
      if (parent !is PsiClass) {
        return Optional.empty()
      }
      val methodNames = findRequiredPropSetterNames(parent)
      if (methodNames.isEmpty()) {
        return Optional.empty()
      }
      return Optional.of(
          MethodChainLookupElement.create(
              delegate,
              createMethod.name,
              methodNames,
              placeholder,
              project,
          ),
      )
    }

    /**
     * Required property setter has different variations: `text()` (main), `textRes()`,
     * `textAttr()`, etc.
     */
    private fun isMainRequiredPropertySetter(
        methodName: String,
        methodAnnotation: RequiredProp,
    ): Boolean {
      return methodName == methodAnnotation.value
    }

    @JvmStatic
    @VisibleForTesting
    internal fun isComponentCreateMethod(method: PsiMethod): Boolean {
      if ("create" != method.name) {
        return false
      }
      if (method.parameters.size != 1) {
        return false
      }
      val parent = method.parent
      if (parent !is PsiClass) {
        return false
      }
      return LithoPluginUtils.isComponentClass(parent)
    }

    @JvmStatic
    @VisibleForTesting
    internal fun findRequiredPropSetterNames(component: PsiClass): List<String> {
      val builder = component.findInnerClassByName("Builder", false) ?: return emptyList()
      return builder.methods.mapNotNull { method ->
        @Suppress("UNCHECKED_CAST")
        val annotation =
            PsiAnnotationProxyUtils.findAnnotationInHierarchy(
                method as PsiModifierListOwner,
                RequiredProp::class.java as Class<java.lang.annotation.Annotation>,
            ) as? RequiredProp ?: return@mapNotNull null
        val methodName = method.name
        if (!isMainRequiredPropertySetter(methodName, annotation)) return@mapNotNull null
        methodName
      }
    }
  }
}
