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

import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.annotations.TreeProp
import com.facebook.litho.intellij.completion.CompletionUtils.METHOD_PARAMETER_ANNOTATION
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.facebook.litho.specmodels.model.DelegateMethodDescription
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions
import com.google.common.annotations.VisibleForTesting
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import java.util.Optional

class LayoutSpecMethodParameterAnnotationsContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, METHOD_PARAMETER_ANNOTATION, Provider.INSTANCE)
  }

  internal class Provider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
      val element = parameters.position
      val insertHandler =
          InsertHandler<LookupElement> { _, item ->
            val data = HashMap<String, String>()
            data[EventLogger.KEY_TARGET] = EventLogger.VALUE_COMPLETION_TARGET_PARAMETER
            data[EventLogger.KEY_CLASS] = item.lookupString
            LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data)
          }
      Optional.ofNullable(PsiTreeUtil.findFirstParent(element, PsiMethod::class.java::isInstance))
          .flatMap { method -> getParameterAnnotations(method as PsiMethod) }
          .map { annotations ->
            ReplacingConsumer(
                result,
                QualifiedNamesFilter(annotations.toMutableSet(), insertHandler),
                false,
            )
          }
          .ifPresent { replacingConsumer ->
            // We want our suggestions at the top, that's why adding them first
            replacingConsumer.filterElement.addRemainingCompletions(
                parameters.position.project,
                result,
            )
            result.runRemainingContributors(parameters, replacingConsumer)
          }
    }

    companion object {
      @JvmField val INSTANCE: CompletionProvider<CompletionParameters> = Provider()

      @JvmField
      @VisibleForTesting
      val LAYOUT_SPEC_DELEGATE_METHOD_TO_PARAMETER_ANNOTATIONS: Map<String, Set<String>> =
          DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP.entries.associate { e ->
            val methodDescription = e.value
            val annotations =
                (methodDescription.interStageInputAnnotations.map { it.typeName } +
                        methodDescription.optionalParameterTypes.mapNotNull { type ->
                          getTypeAnnotation(type)?.typeName
                        })
                    .toSet()
            e.key.typeName to annotations
          }

      private fun getTypeAnnotation(
          type: DelegateMethodDescription.OptionalParameterType,
      ): Class<out Annotation>? {
        return when (type) {
          DelegateMethodDescription.OptionalParameterType.PROP -> {
            Prop::class.java
          }
          DelegateMethodDescription.OptionalParameterType.STATE -> {
            State::class.java
          }
          DelegateMethodDescription.OptionalParameterType.TREE_PROP -> {
            TreeProp::class.java
          }
          DelegateMethodDescription.OptionalParameterType.PARAM -> {
            Param::class.java
          }
          DelegateMethodDescription.OptionalParameterType.CACHED_VALUE -> {
            CachedValue::class.java
          }
          else -> {
            null
          }
        }
      }

      private fun getParameterAnnotations(method: PsiMethod): Optional<Set<String>> {
        return method.annotations
            .asSequence()
            .map { it.qualifiedName }
            .mapNotNull { LAYOUT_SPEC_DELEGATE_METHOD_TO_PARAMETER_ANNOTATIONS[it] }
            .firstOrNull()
            .let { Optional.ofNullable(it) }
      }
    }
  }
}
