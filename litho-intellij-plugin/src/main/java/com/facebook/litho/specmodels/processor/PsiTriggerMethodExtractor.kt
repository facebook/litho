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

import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.EventDeclarationModel
import com.facebook.litho.specmodels.model.EventMethod
import com.facebook.litho.specmodels.model.MethodParamModel
import com.facebook.litho.specmodels.model.SpecMethodModel
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression

@Suppress("UNCHECKED_CAST")
object PsiTriggerMethodExtractor {

  @JvmStatic
  fun getOnTriggerMethods(
      psiClass: PsiClass,
      permittedInterStageInputAnnotations: List<Class<out Annotation>>,
      permittedPrepareInterStageInputAnnotations: List<Class<out Annotation>>,
  ): ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> {
    val delegateMethods = mutableListOf<SpecMethodModel<EventMethod, EventDeclarationModel>>()

    for (psiMethod in psiClass.methods) {
      val onTriggerAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(
              psiMethod,
              OnTrigger::class.java as Class<java.lang.annotation.Annotation>,
          )
      if (onTriggerAnnotation != null) {
        val methodParams: List<MethodParamModel> =
            PsiMethodExtractorUtils.getMethodParams(
                psiMethod,
                TriggerMethodExtractor.getPermittedMethodParamAnnotations(
                    permittedInterStageInputAnnotations,
                    permittedPrepareInterStageInputAnnotations,
                ),
                permittedInterStageInputAnnotations,
                permittedPrepareInterStageInputAnnotations,
                ImmutableList.of(),
            )

        val psiOnTriggerAnnotation =
            checkNotNull(AnnotationUtil.findAnnotation(psiMethod, OnTrigger::class.java.name))
        val valuePair =
            checkNotNull(AnnotationUtil.findDeclaredAttribute(psiOnTriggerAnnotation, "value"))
        val valueClassExpression = checkNotNull(valuePair.value) as PsiClassObjectAccessExpression

        // Reuse EventMethodModel and EventDeclarationModel because we are capturing the same info
        val returnType = checkNotNull(psiMethod.returnType)
        val eventMethod = SpecMethodModel<EventMethod, EventDeclarationModel>(
            ImmutableList.of(),
            PsiModifierExtractor.extractModifiers(psiMethod.modifierList),
            psiMethod.name,
            PsiTypeUtils.generateTypeSpec(returnType),
            ImmutableList.copyOf(PsiMethodExtractorUtils.getTypeVariables(psiMethod)),
            ImmutableList.copyOf(methodParams),
            psiMethod,
            null,
            PsiEventDeclarationsExtractor.getEventDeclarationModel(valueClassExpression),
        )
        delegateMethods.add(eventMethod)
      }
    }

    return ImmutableList.copyOf(delegateMethods)
  }
}
