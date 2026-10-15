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

import com.facebook.litho.annotations.OnEnteredRange
import com.facebook.litho.annotations.OnExitedRange
import com.facebook.litho.annotations.OnRegisterRanges
import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.EventMethod
import com.facebook.litho.specmodels.model.MethodParamModel
import com.facebook.litho.specmodels.model.SpecMethodModel
import com.facebook.litho.specmodels.model.WorkingRangeDeclarationModel
import com.facebook.litho.specmodels.model.WorkingRangeMethodModel
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

@Suppress("UNCHECKED_CAST")
internal object PsiWorkingRangesMethodExtractor {

  @JvmStatic
  fun getRegisterMethod(
      psiClass: PsiClass,
      permittedInterStageInputAnnotations: List<Class<out Annotation>>,
      permittedPrepareInterStageInputAnnotations: List<Class<out Annotation>>,
  ): SpecMethodModel<EventMethod, Void>? {
    for (psiMethod in psiClass.methods) {
      val onRegisterRangesAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(
              psiMethod,
              OnRegisterRanges::class.java as Class<java.lang.annotation.Annotation>,
          )
      if (onRegisterRangesAnnotation != null) {
        val methodParams: List<MethodParamModel> =
            PsiMethodExtractorUtils.getMethodParams(
                psiMethod,
                DelegateMethodExtractor.getPermittedMethodParamAnnotations(
                    permittedInterStageInputAnnotations,
                    permittedPrepareInterStageInputAnnotations,
                ),
                permittedInterStageInputAnnotations,
                permittedPrepareInterStageInputAnnotations,
                ImmutableList.of(),
            )

        val returnType = requireNotNull(psiMethod.returnType)
        return SpecMethodModel.builder<EventMethod, Void>()
            .annotations(ImmutableList.of())
            .modifiers(PsiModifierExtractor.extractModifiers(psiMethod.modifierList))
            .name(psiMethod.name)
            .returnTypeSpec(PsiTypeUtils.generateTypeSpec(returnType))
            .typeVariables(
                ImmutableList.copyOf(PsiMethodExtractorUtils.getTypeVariables(psiMethod)),
            )
            .methodParams(ImmutableList.copyOf(methodParams))
            .representedObject(psiMethod)
            .build()
      }
    }
    return null
  }

  @JvmStatic
  fun getRangesMethods(
      psiClass: PsiClass,
      permittedInterStageInputAnnotations: List<Class<out Annotation>>,
      permittedPrepareInterStageInputAnnotations: List<Class<out Annotation>>,
  ): ImmutableList<WorkingRangeMethodModel> {
    val models = HashMap<String, WorkingRangeMethodModel>()

    for (psiMethod in psiClass.methods) {
      // OnEnteredRange
      val enteredRangeAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(
              psiMethod,
              OnEnteredRange::class.java as Class<java.lang.annotation.Annotation>,
          ) as? OnEnteredRange
      if (enteredRangeAnnotation != null) {
        val enteredRangeMethod = generateWorkingRangeMethod(
            psiMethod,
            permittedInterStageInputAnnotations,
            permittedPrepareInterStageInputAnnotations,
            OnEnteredRange::class.java.name,
        )
        val name = enteredRangeAnnotation.name
        models.putIfAbsent(name, WorkingRangeMethodModel(name))
        requireNotNull(models[name]).enteredRangeModel = enteredRangeMethod
      }

      // OnExitedRange
      val exitedRangeAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(
              psiMethod,
              OnExitedRange::class.java as Class<java.lang.annotation.Annotation>,
          ) as? OnExitedRange
      if (exitedRangeAnnotation != null) {
        val exitedRangeMethod = generateWorkingRangeMethod(
            psiMethod,
            permittedInterStageInputAnnotations,
            permittedPrepareInterStageInputAnnotations,
            OnExitedRange::class.java.name,
        )
        val name = exitedRangeAnnotation.name
        models.putIfAbsent(name, WorkingRangeMethodModel(name))
        requireNotNull(models[name]).exitedRangeModel = exitedRangeMethod
      }
    }
    return ImmutableList.copyOf(ArrayList(models.values))
  }

  private fun generateWorkingRangeMethod(
      psiMethod: PsiMethod,
      permittedInterStageInputAnnotations: List<Class<out Annotation>>,
      permittedPrepareInterStageInputAnnotations: List<Class<out Annotation>>,
      annotationQualifiedName: String,
  ): SpecMethodModel<EventMethod, WorkingRangeDeclarationModel> {
    val methodParams: List<MethodParamModel> =
        PsiMethodExtractorUtils.getMethodParams(
            psiMethod,
            DelegateMethodExtractor.getPermittedMethodParamAnnotations(
                permittedInterStageInputAnnotations,
                permittedPrepareInterStageInputAnnotations,
            ),
            permittedInterStageInputAnnotations,
            permittedPrepareInterStageInputAnnotations,
            ImmutableList.of(),
        )

    val psiAnnotation =
        requireNotNull(AnnotationUtil.findAnnotation(psiMethod, annotationQualifiedName))
    val valuePair = AnnotationUtil.findDeclaredAttribute(psiAnnotation, "name")

    val returnType = requireNotNull(psiMethod.returnType)
    return SpecMethodModel.builder<EventMethod, WorkingRangeDeclarationModel>()
        .annotations(ImmutableList.of())
        .modifiers(PsiModifierExtractor.extractModifiers(psiMethod.modifierList))
        .name(psiMethod.name)
        .returnTypeSpec(PsiTypeUtils.generateTypeSpec(returnType))
        .typeVariables(ImmutableList.copyOf(PsiMethodExtractorUtils.getTypeVariables(psiMethod)))
        .methodParams(ImmutableList.copyOf(methodParams))
        .representedObject(psiMethod)
        .typeModel(
            WorkingRangeDeclarationModel(
                requireNotNull(valuePair).literalValue,
                valuePair.nameIdentifier,
            ),
        )
        .build()
  }
}
