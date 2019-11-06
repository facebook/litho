/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.FieldModel;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.util.PsiTypesUtil;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public class PsiEventDeclarationsExtractor {

  public static ImmutableList<EventDeclarationModel> getEventDeclarations(PsiClass psiClass) {
    final PsiAnnotation layoutSpecAnnotation =
        AnnotationUtil.findAnnotation(psiClass, LayoutSpec.class.getName());
    if (layoutSpecAnnotation == null) {
      throw new RuntimeException("LayoutSpec annotation not found on class");
    }

    PsiAnnotationMemberValue psiAnnotationMemberValue =
        layoutSpecAnnotation.findAttributeValue("events");

    ArrayList<EventDeclarationModel> eventDeclarationModels = new ArrayList<>();
    if (psiAnnotationMemberValue instanceof PsiArrayInitializerMemberValue) {
      PsiArrayInitializerMemberValue value =
          (PsiArrayInitializerMemberValue) psiAnnotationMemberValue;
      for (PsiAnnotationMemberValue annotationMemberValue : value.getInitializers()) {
        PsiClassObjectAccessExpression accessExpression =
            (PsiClassObjectAccessExpression) annotationMemberValue;
        eventDeclarationModels.add(getEventDeclarationModel(accessExpression));
      }
    } else if (psiAnnotationMemberValue instanceof PsiClassObjectAccessExpression) {
      eventDeclarationModels.add(
          getEventDeclarationModel((PsiClassObjectAccessExpression) psiAnnotationMemberValue));
    }

    return ImmutableList.copyOf(eventDeclarationModels);
  }

  static EventDeclarationModel getEventDeclarationModel(
      PsiClassObjectAccessExpression psiExpression) {
    PsiType valueType = psiExpression.getOperand().getType();
    PsiClass valueClass = PsiTypesUtil.getPsiClass(valueType);

    return new EventDeclarationModel(
        PsiTypeUtils.guessClassName(valueType.getCanonicalText()),
        getReturnType(valueClass),
        getFields(valueClass),
        psiExpression);
  }

  /**
   * Finds return type for the provided Event class.
   *
   * @param eventClass the class representing Litho Event. It should contain {@link Event}
   *     annotation on it.
   * @return the return type of the Event; {@link TypeName#VOID} if it is not defined; null if the
   *     provided class is not Event class.
   */
  @Nullable
  static TypeName getReturnType(@Nullable PsiClass eventClass) {
    return PsiTypeUtils.getTypeName(getReturnPsiType(eventClass));
  }

  public static PsiType getReturnPsiType(@Nullable PsiClass eventClass) {
    return Optional.ofNullable(eventClass)
        .map(cls -> AnnotationUtil.findAnnotation(eventClass, Event.class.getTypeName()))
        .map(psiAnnotation -> psiAnnotation.findAttributeValue("returnType"))
        .filter(PsiClassObjectAccessExpression.class::isInstance)
        .map(PsiClassObjectAccessExpression.class::cast)
        .map(PsiClassObjectAccessExpression::getOperand)
        .map(PsiTypeElement::getType)
        .orElse(PsiType.VOID);
  }

  static ImmutableList<FieldModel> getFields(@Nullable PsiClass psiClass) {
    if (psiClass == null) {
      return ImmutableList.of();
    }
    final List<FieldModel> fieldModels = new ArrayList<>();
    for (PsiField psiField : psiClass.getFields()) {
      fieldModels.add(
          new FieldModel(
              FieldSpec.builder(
                      PsiTypeUtils.getTypeName(psiField.getType()),
                      psiField.getName(),
                      PsiModifierExtractor.extractModifiers(psiField))
                  .build(),
              psiField));
    }

    return ImmutableList.copyOf(fieldModels);
  }
}
