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

import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.OnUpdateStateWithTransition
import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.MethodParamModel
import com.facebook.litho.specmodels.model.SpecMethodModel
import com.facebook.litho.specmodels.model.UpdateStateMethod
import com.intellij.psi.PsiClass
import com.squareup.javapoet.TypeVariableName

@Suppress("UNCHECKED_CAST")
object PsiUpdateStateMethodExtractor {

  @JvmStatic
  fun getOnUpdateStateMethods(
      psiClass: PsiClass,
      permittedInterStageInputAnnotations: List<Class<out Annotation>>,
      permittedPrepareInterStageInputAnnotations: List<Class<out Annotation>>,
      isTransition: Boolean,
  ): ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> {
    val delegateMethods = mutableListOf<SpecMethodModel<UpdateStateMethod, Void>>()

    for (psiMethod in psiClass.methods) {
      val onUpdateStateAnnotation =
          if (isTransition) {
            PsiAnnotationProxyUtils.findAnnotationInHierarchy(
                psiMethod,
                OnUpdateStateWithTransition::class.java as Class<java.lang.annotation.Annotation>,
            )
          } else {
            PsiAnnotationProxyUtils.findAnnotationInHierarchy(
                psiMethod,
                OnUpdateState::class.java as Class<java.lang.annotation.Annotation>,
            )
          }

      if (onUpdateStateAnnotation != null) {
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
        val delegateMethod =
            SpecMethodModel.builder<UpdateStateMethod, Void>()
                .annotations(ImmutableList.of(onUpdateStateAnnotation as Annotation))
                .modifiers(PsiModifierExtractor.extractModifiers(psiMethod.modifierList))
                .name(psiMethod.name)
                .returnTypeSpec(PsiTypeUtils.generateTypeSpec(returnType))
                .typeVariables(ImmutableList.of<TypeVariableName>())
                .methodParams(ImmutableList.copyOf(methodParams))
                .representedObject(psiMethod)
                .build()
        delegateMethods.add(delegateMethod)
      }
    }

    return ImmutableList.copyOf(delegateMethods)
  }
}
