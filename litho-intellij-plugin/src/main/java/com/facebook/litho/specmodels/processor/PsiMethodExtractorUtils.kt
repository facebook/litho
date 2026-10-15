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

import com.facebook.litho.specmodels.model.MethodParamModel
import com.facebook.litho.specmodels.model.MethodParamModelFactory
import com.facebook.litho.specmodels.processor.MethodExtractorUtils.COMPONENTS_PACKAGE
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import java.util.Arrays
import java.util.stream.Collectors

/** Extracts methods from the given input. */
object PsiMethodExtractorUtils {

  @JvmStatic
  fun getTypeVariables(method: PsiMethod): List<TypeVariableName> {
    return Arrays.stream(method.typeParameters)
        .map { psiTypeParameter -> toTypeVariableName(psiTypeParameter) }
        .collect(Collectors.toList())
  }

  private fun toTypeVariableName(psiTypeParameter: PsiTypeParameter): TypeVariableName {
    val typeName = psiTypeParameter.name
    val typeBounds =
        Arrays.stream(psiTypeParameter.bounds)
            .filter { bound -> bound is PsiType }
            .map { bound -> PsiTypeUtils.getTypeName(bound as PsiType) }
            .toArray { size -> arrayOfNulls<TypeName>(size) }
    return TypeVariableName.get(typeName, *typeBounds)
  }

  @Suppress("UNCHECKED_CAST")
  @JvmStatic
  fun getMethodParams(
      method: PsiMethod,
      permittedAnnotations: List<Class<out Annotation>>,
      permittedInterStageInputAnnotations: List<Class<out Annotation>>,
      permittedPrepareInterStageInputAnnotations: List<Class<out Annotation>>,
      delegateMethodAnnotationsThatSkipDiffModels: List<Class<out Annotation>>,
  ): List<MethodParamModel> {
    val methodParamModels = ArrayList<MethodParamModel>()
    val params = method.parameterList.parameters

    for (param in params) {
      methodParamModels.add(
          MethodParamModelFactory.create(
              PsiTypeUtils.generateTypeSpec(param.type),
              param.name,
              getLibraryAnnotations(
                  param,
                  permittedAnnotations as List<Class<out java.lang.annotation.Annotation>>,
              ),
              getExternalAnnotations(param),
              permittedInterStageInputAnnotations,
              permittedPrepareInterStageInputAnnotations,
              canCreateDiffModels(
                  method,
                  delegateMethodAnnotationsThatSkipDiffModels
                      as List<Class<out java.lang.annotation.Annotation>>,
              ),
              param,
          ),
      )
    }

    return methodParamModels
  }

  private fun canCreateDiffModels(
      method: PsiMethod,
      delegateMethodAnnotationsThatSkipDiffModels: List<Class<out java.lang.annotation.Annotation>>,
  ): Boolean {
    for (delegate in delegateMethodAnnotationsThatSkipDiffModels) {
      @Suppress("UNCHECKED_CAST")
      if (
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(
              method,
              delegate as Class<java.lang.annotation.Annotation>,
          ) != null
      ) {
        return false
      }
    }
    return true
  }

  private fun getLibraryAnnotations(
      param: PsiParameter,
      permittedAnnotations: List<Class<out java.lang.annotation.Annotation>>,
  ): List<Annotation> {
    val paramAnnotations = mutableListOf<Annotation>()
    for (possibleMethodParamAnnotation in permittedAnnotations) {
      @Suppress("UNCHECKED_CAST")
      val paramAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(
              param,
              possibleMethodParamAnnotation as Class<java.lang.annotation.Annotation>,
          )
      if (paramAnnotation != null) {
        paramAnnotations.add(paramAnnotation as Annotation)
      }
    }
    return paramAnnotations
  }

  private fun getExternalAnnotations(param: PsiParameter): List<AnnotationSpec> {
    val annotationsOnParam = AnnotationUtil.getAllAnnotations(param, false, null)
    val annotations = ArrayList<AnnotationSpec>()

    for (annotationOnParam in annotationsOnParam) {
      val qualifiedName = annotationOnParam.qualifiedName
      if (qualifiedName == null || qualifiedName.startsWith(COMPONENTS_PACKAGE)) {
        continue
      }

      val annotationSpec = AnnotationSpec.builder(PsiTypeUtils.guessClassName(qualifiedName))

      val paramAttributes = annotationOnParam.parameterList.attributes
      for (attribute in paramAttributes) {
        val detachedValue = attribute.detachedValue
        if (attribute.name != null && detachedValue != null) {
          annotationSpec.addMember(attribute.name, detachedValue.text)
        }
      }

      annotations.add(annotationSpec.build())
    }

    return annotations
  }
}
