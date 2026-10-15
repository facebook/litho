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

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResult
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Consumer
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtTypeReference

class KComponentRequiredPropMethodContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, CompletionUtils.IN_KT_EXPR, KComponentRequiredPropMethodProvider)
  }

  object KComponentRequiredPropMethodProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
      val dummyCompletionResultSet =
          DummyCompletionResultSet(result.prefixMatcher, Consumer {}, kotlinCompletionContributor)
      kotlinCompletionContributor.fillCompletionVariants(parameters, dummyCompletionResultSet)

      dummyCompletionResultSet.completionLookupElements.forEach { result.addElement(it) }
    }
  }

  private class DummyCompletionResultSet(
      prefixMatcher: PrefixMatcher,
      consumer: Consumer<in CompletionResult>,
      contributor: CompletionContributor,
  ) : CompletionResultSet(prefixMatcher, consumer, contributor) {

    val completionLookupElements: MutableList<LookupElement> = ArrayList()

    override fun addElement(element: LookupElement) {
      val psiElement = element.psiElement ?: return
      val lookupElement: LookupElement
      if (checkIsComponentWrapper(psiElement)) {
        val lithoKotlinFunction = psiElement as KtNamedFunction
        val lookupString = createKotlinCompletionString(lithoKotlinFunction)
        val name = lithoKotlinFunction.name ?: return
        lookupElement =
            createKotlinCompletionLookupElement(lithoKotlinFunction, lookupString, element, name)
      } else if (checkIsKComponent(psiElement)) {
        val lithoKotlinClass = psiElement as KtClass
        val lookupString = createKotlinCompletionStringForKComponent(lithoKotlinClass) ?: return
        val name = lithoKotlinClass.name ?: return
        lookupElement =
            createKotlinCompletionLookupElement(lithoKotlinClass, lookupString, element, name)
      } else {
        return
      }
      completionLookupElements.add(lookupElement)
    }

    override fun withPrefixMatcher(matcher: PrefixMatcher): CompletionResultSet {
      return this
    }

    override fun withPrefixMatcher(prefix: String): CompletionResultSet {
      return this
    }

    override fun withRelevanceSorter(sorter: CompletionSorter): CompletionResultSet {
      return this
    }

    override fun addLookupAdvertisement(text: String): Unit = Unit

    override fun caseInsensitive(): CompletionResultSet {
      return this
    }

    override fun restartCompletionOnPrefixChange(prefixCondition: ElementPattern<String>): Unit =
        Unit

    override fun restartCompletionWhenNothingMatches(): Unit = Unit
  }

  companion object {
    private val kotlinCompletionContributor: CompletionContributor =
        createKotlinFirCompletionContributor()

    private fun createKotlinFirCompletionContributor(): CompletionContributor {
      // KotlinFirCompletionContributor moved packages across IDE versions:
      // AI-243: org.jetbrains.kotlin.idea.completion
      // IU-261: org.jetbrains.kotlin.idea.completion.impl.k2
      val classNames = arrayOf(
          "org.jetbrains.kotlin.idea.completion.impl.k2.KotlinFirCompletionContributor",
          "org.jetbrains.kotlin.idea.completion.KotlinFirCompletionContributor",
      )
      for (className in classNames) {
        try {
          return Class.forName(className).getDeclaredConstructor().newInstance()
              as CompletionContributor
        } catch (_: Exception) {
          // Expected: trying multiple package locations across IDE versions
        }
      }
      throw RuntimeException("Could not instantiate KotlinFirCompletionContributor")
    }

    @JvmStatic
    internal fun checkIsKComponent(psiElement: PsiElement?): Boolean {
      if (psiElement !is KtClass) {
        return false
      }
      val superTypeList = psiElement.getSuperTypeList() ?: return false
      val ktSuperTypeCallEntry =
          PsiTreeUtil.getChildOfType(superTypeList, KtSuperTypeCallEntry::class.java)
              ?: return false
      return ktSuperTypeCallEntry.text == "KComponent()"
    }

    @JvmStatic
    internal fun checkIsComponentWrapper(psiElement: PsiElement?): Boolean {
      if (psiElement !is KtNamedFunction) { // Kotlin Litho components are functions
        return false
      }
      val referenceTypes =
          PsiTreeUtil.getChildrenOfTypeAsList(psiElement, KtTypeReference::class.java) // 2 Types:
      if (referenceTypes.size < 2) {
        return false
      }
      return "ComponentScope" == referenceTypes[0].text
    }

    @JvmStatic
    internal fun createKotlinCompletionString(lithoKotlinFunction: KtNamedFunction): String {
      val parameters =
          lithoKotlinFunction.valueParameters
              .filter { !it.hasDefaultValue() }
              .map { it.name + " = " }
      return lithoKotlinFunction.name + "(" + parameters.joinToString(", ") + " )"
    }

    @JvmStatic
    internal fun createKotlinCompletionStringForKComponent(
        lithoKotlinClass: KtClass,
    ): String? {
      val ktPrimaryConstructor =
          PsiTreeUtil.getChildOfType(lithoKotlinClass, KtPrimaryConstructor::class.java)
              ?: return null
      val ktParameterList = ktPrimaryConstructor.valueParameterList ?: return null
      val parameterList = ktParameterList.parameters
      if (parameterList.isEmpty()) {
        return null
      }
      val parameters =
          parameterList.filter { !it.hasDefaultValue() }.map { it.name + " = " }.toMutableList()

      var addKotlinLambda = false
      val typeElement = parameterList[parameterList.size - 1].typeReference
      if (typeElement != null && typeElement.typeElement is KtFunctionType) {
        addKotlinLambda = true
        // remove final param as we turn it into lambda expression
        parameters.removeAt(parameters.size - 1)
      }

      val completionString = lithoKotlinClass.name + "(" + parameters.joinToString(", ") + ")"
      if (addKotlinLambda) {
        return "$completionString {}"
      }
      return completionString
    }

    @JvmStatic
    internal fun createKotlinCompletionLookupElement(
        lithoKotlinElement: PsiElement,
        joinedParameters: String,
        element: LookupElement,
        name: String,
    ): LookupElement {
      val lookupElement =
          LookupElementBuilder.create(joinedParameters)
              .withBoldness(true)
              .withPresentableText(name)
              .appendTailText(joinedParameters.replaceFirst(name, ""), true)
              .withLookupString(name)
              .withTypeText("Litho $name")
              .withIcon(lithoKotlinElement.getIcon(1))
              .withInsertHandler { context, lookupItem ->
                // We use this insert handler to get the import statement added automatically. We
                // then delete the inserted lookup element and add out own Completion result.
                element.handleInsert(context)
                context.document.deleteString(context.startOffset, context.tailOffset)
                context.document.insertString(context.tailOffset, lookupItem.lookupString)
                context.commitDocument()
                context.editor.caretModel.moveToOffset(context.tailOffset - 1)
              }
      return PrioritizedLookupElement.withPriority(lookupElement, Integer.MAX_VALUE.toDouble())
    }
  }
}
