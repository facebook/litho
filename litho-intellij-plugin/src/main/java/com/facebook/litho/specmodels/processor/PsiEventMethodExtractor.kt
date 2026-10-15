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

package com.facebook.litho.specmodels.processor

import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.EventDeclarationModel
import com.facebook.litho.specmodels.model.EventMethod
import com.facebook.litho.specmodels.model.MethodParamModel
import com.facebook.litho.specmodels.model.SpecMethodModel
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression

/** Extracts event methods from the given input. */
object PsiEventMethodExtractor {

  // Public (not internal) because this is called from Java files. Kotlin's `internal` mangles the
  // method name in bytecode, making it invisible to Java callers.
  @JvmStatic
  fun getOnEventMethods(
      psiClass: PsiClass,
      permittedInterStageInputAnnotations: List<Class<out Annotation>>,
      permittedPrepareInterStageInputAnnotations: List<Class<out Annotation>>,
  ): ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> {
    val delegateMethods = mutableListOf<SpecMethodModel<EventMethod, EventDeclarationModel>>()

    for (psiMethod in psiClass.methods) {
      val onEventAnnotation =
          AnnotationUtil.findAnnotation(psiMethod, OnEvent::class.java.name) ?: continue

      // Non-null cast to match original Java cast behavior. In practice, value is never null
      // for @OnEvent annotations since `value` is a required attribute.
      val accessExpression =
          onEventAnnotation.findAttributeValue("value") as PsiClassObjectAccessExpression
      val methodParams: List<MethodParamModel> =
          PsiMethodExtractorUtils.getMethodParams(
              psiMethod,
              EventMethodExtractor.getPermittedMethodParamAnnotations(
                  permittedInterStageInputAnnotations,
                  permittedPrepareInterStageInputAnnotations,
              ),
              permittedInterStageInputAnnotations,
              permittedPrepareInterStageInputAnnotations,
              ImmutableList.of(),
          )

      val eventMethod = SpecMethodModel<EventMethod, EventDeclarationModel>(
          ImmutableList.of(),
          PsiModifierExtractor.extractModifiers(psiMethod.modifierList),
          psiMethod.name,
          // returnType is null only for constructors, but @OnEvent methods are never
          // constructors, so this is safe.
          PsiTypeUtils.generateTypeSpec(requireNotNull(psiMethod.returnType)),
          ImmutableList.copyOf(PsiMethodExtractorUtils.getTypeVariables(psiMethod)),
          ImmutableList.copyOf(methodParams),
          psiMethod,
          null,
          PsiEventDeclarationsExtractor.getEventDeclarationModel(accessExpression),
      )
      delegateMethods.add(eventMethod)
    }

    return ImmutableList.copyOf(delegateMethods)
  }
}
