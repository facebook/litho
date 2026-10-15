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

import com.facebook.litho.annotations.Event
import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.EventDeclarationModel
import com.facebook.litho.specmodels.model.FieldModel
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeName
import java.util.Optional

object PsiEventDeclarationsExtractor {

  @JvmStatic
  fun getEventDeclarations(
      psiClass: PsiClass,
      annotationClass: Class<*>,
  ): ImmutableList<EventDeclarationModel> {
    val specAnnotation =
        AnnotationUtil.findAnnotation(psiClass, annotationClass.name)
            ?: throw RuntimeException("${annotationClass.name} annotation not found on class")

    val psiAnnotationMemberValue: PsiAnnotationMemberValue? =
        specAnnotation.findAttributeValue("events")

    val eventDeclarationModels = ArrayList<EventDeclarationModel>()
    if (psiAnnotationMemberValue is PsiArrayInitializerMemberValue) {
      for (annotationMemberValue in psiAnnotationMemberValue.initializers) {
        val accessExpression = annotationMemberValue as PsiClassObjectAccessExpression
        eventDeclarationModels.add(getEventDeclarationModel(accessExpression))
      }
    } else if (psiAnnotationMemberValue is PsiClassObjectAccessExpression) {
      eventDeclarationModels.add(getEventDeclarationModel(psiAnnotationMemberValue))
    }

    return ImmutableList.copyOf(eventDeclarationModels)
  }

  @JvmStatic
  fun getEventDeclarationModel(
      psiExpression: PsiClassObjectAccessExpression,
  ): EventDeclarationModel {
    val valueType = psiExpression.operand.type
    val valueClass = PsiTypesUtil.getPsiClass(valueType)

    return EventDeclarationModel(
        PsiTypeUtils.guessClassName(valueType.canonicalText),
        getReturnType(valueClass),
        getFields(valueClass),
        psiExpression,
    )
  }

  /**
   * Finds return type for the provided Event class.
   *
   * @param eventClass the class representing Litho Event. It should contain [Event] annotation on
   *   it.
   * @return the return type of the Event; [TypeName.VOID] if it is not defined; null if the
   *   provided class is not Event class.
   */
  @JvmStatic
  fun getReturnType(eventClass: PsiClass?): TypeName? {
    return PsiTypeUtils.getTypeName(getReturnPsiType(eventClass))
  }

  @JvmStatic
  fun getReturnPsiType(eventClass: PsiClass?): PsiType {
    return Optional.ofNullable(eventClass)
        .map { cls -> AnnotationUtil.findAnnotation(cls, Event::class.java.typeName) }
        .map { psiAnnotation -> psiAnnotation?.findAttributeValue("returnType") }
        .filter { it is PsiClassObjectAccessExpression }
        .map { it as PsiClassObjectAccessExpression }
        .map { it.operand }
        .map { it.type }
        .orElse(PsiType.VOID)
  }

  @JvmStatic
  fun getFields(psiClass: PsiClass?): ImmutableList<FieldModel> {
    if (psiClass == null) {
      return ImmutableList.of()
    }
    val fieldModels = ArrayList<FieldModel>()
    for (psiField in psiClass.fields) {
      fieldModels.add(
          FieldModel(
              FieldSpec.builder(
                      PsiTypeUtils.getTypeName(psiField.type),
                      psiField.name,
                      *PsiModifierExtractor.extractModifiers(psiField),
                  )
                  .build(),
              psiField,
          ),
      )
    }

    return ImmutableList.copyOf(fieldModels)
  }
}
