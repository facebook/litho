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

import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.DelegateMethod
import com.facebook.litho.specmodels.model.MethodParamModel
import com.facebook.litho.specmodels.model.SpecMethodModel
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.squareup.javapoet.TypeVariableName

/** Extracts delegate methods from the given input. */
object PsiDelegateMethodExtractor {

  @JvmStatic
  fun getDelegateMethods(
      psiClass: PsiClass,
      permittedMethodAnnotations: List<Class<out Annotation>>,
      permittedInterStageInputAnnotations: List<Class<out Annotation>>,
      permittedPrepareInterStageInputAnnotations: List<Class<out Annotation>>,
      delegateMethodAnnotationsThatSkipDiffModels: List<Class<out Annotation>>,
  ): ImmutableList<SpecMethodModel<DelegateMethod, Void>> {
    val delegateMethods = mutableListOf<SpecMethodModel<DelegateMethod, Void>>()

    for (psiMethod in psiClass.methods) {
      val methodAnnotations = getMethodAnnotations(psiMethod, permittedMethodAnnotations)

      if (methodAnnotations.isNotEmpty()) {
        val methodParams: List<MethodParamModel> =
            PsiMethodExtractorUtils.getMethodParams(
                psiMethod,
                DelegateMethodExtractor.getPermittedMethodParamAnnotations(
                    permittedInterStageInputAnnotations,
                    permittedPrepareInterStageInputAnnotations,
                ),
                permittedInterStageInputAnnotations,
                permittedPrepareInterStageInputAnnotations,
                delegateMethodAnnotationsThatSkipDiffModels,
            )

        val delegateMethod = SpecMethodModel<DelegateMethod, Void>(
            ImmutableList.copyOf(methodAnnotations),
            PsiModifierExtractor.extractModifiers(psiMethod.modifierList),
            psiMethod.name,
            PsiTypeUtils.generateTypeSpec(requireNotNull(psiMethod.returnType)),
            ImmutableList.of<TypeVariableName>(),
            ImmutableList.copyOf(methodParams),
            psiMethod,
            null,
            null,
        )
        delegateMethods.add(delegateMethod)
      }
    }

    return ImmutableList.copyOf(delegateMethods)
  }

  private fun getMethodAnnotations(
      psiMethod: PsiMethod,
      permittedMethodAnnotations: List<Class<out Annotation>>,
  ): List<Annotation> {
    val methodAnnotations = mutableListOf<Annotation>()
    for (possibleDelegateMethodAnnotation in permittedMethodAnnotations) {
      @Suppress("UNCHECKED_CAST")
      val methodAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(
              psiMethod,
              possibleDelegateMethodAnnotation as Class<java.lang.annotation.Annotation>,
          )
      if (methodAnnotation != null) {
        methodAnnotations.add(methodAnnotation as Annotation)
      }
    }
    return methodAnnotations
  }
}
