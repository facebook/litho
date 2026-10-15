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

import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.intellij.LithoPluginUtils
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.java.JavaLanguage
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import java.util.StringJoiner
import java.util.function.Predicate

/** Offers completion for the `@Prop` and `@State` method parameters in the Litho Spec class. */
class StatePropCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, codeReferencePattern(), typeCompletionProvider())
  }

  companion object {
    private fun codeReferencePattern() =
        PlatformPatterns.psiElement(PsiIdentifier::class.java)
            .withParent(PsiJavaCodeReferenceElement::class.java)
            .withLanguage(JavaLanguage.INSTANCE)

    private fun typeCompletionProvider(): CompletionProvider<CompletionParameters> {
      return object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            completionParameters: CompletionParameters,
            processingContext: ProcessingContext,
            completionResultSet: CompletionResultSet,
        ) {
          val element = completionParameters.position

          // Method parameter type in the Spec class
          // PsiIdentifier -> PsiJavaCodeReferenceElement -> PsiTypeElement -> PsiMethod -> PsiClass
          val typeElement =
              PsiTreeUtil.getParentOfType(element, PsiTypeElement::class.java) ?: return
          val containingMethod =
              PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return
          val cls = containingMethod.containingClass
          if (!LithoPluginUtils.isLithoSpec(cls)) {
            return
          }
          if (cls == null) {
            return
          }

          // @Prop or @State annotation
          val parameterModifiers =
              PsiTreeUtil.getPrevSiblingOfType(typeElement, PsiModifierList::class.java) ?: return
          if (parameterModifiers.findAnnotation(Prop::class.java.name) != null) {
            addCompletionResult(
                completionResultSet,
                containingMethod,
                cls.methods,
                LithoPluginUtils::isProp,
            )
          } else if (parameterModifiers.findAnnotation(State::class.java.name) != null) {
            addCompletionResult(
                completionResultSet,
                containingMethod,
                cls.methods,
                LithoPluginUtils::isState,
            )
          }
        }
      }
    }

    // Package-private to be accessed from the anonymous inner class
    @JvmStatic
    internal fun addCompletionResult(
        completionResultSet: CompletionResultSet,
        currentMethod: PsiMethod,
        allMethods: Array<PsiMethod>,
        annotationCheck: Predicate<PsiParameter>,
    ) {
      // Don't propose completion with current method parameters
      val excludingParameters =
          currentMethod.parameterList.parameters
              .filter { annotationCheck.test(it) }
              .mapNotNull { it.name }
              .toSet()

      LithoPluginUtils.getPsiParameterStream(currentMethod, allMethods)
          .filter { annotationCheck.test(it) }
          .filter { !excludingParameters.contains(it.name) }
          .map { createCompletionResult(it) }
          .forEach { completionResultSet.addElement(it) }
    }

    // Package-private to be used in anonymous Function
    @JvmStatic
    internal fun createCompletionResult(parameter: PsiParameter): LookupElement {
      return LookupElementBuilder.create(
          StringJoiner(" ").add(parameter.type.presentableText).add(parameter.name).toString(),
      )
    }
  }
}
