/*
 * Copyright 2004-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.specmodels.processor;

import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.FieldModel;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiType;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class PsiEventDeclarationsExtractor {

  public static ImmutableList<EventDeclarationModel> getEventDeclarations(
      Project project, PsiClass psiClass) {
    final PsiAnnotation layoutSpecAnnotation =
        AnnotationUtil.findAnnotation(psiClass, "com.facebook.litho.annotations.LayoutSpec");
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
        eventDeclarationModels.add(getEventDeclarationModel(project, accessExpression));
      }
    } else {
      PsiClassObjectAccessExpression accessExpression =
          (PsiClassObjectAccessExpression) psiAnnotationMemberValue;
      eventDeclarationModels.add(getEventDeclarationModel(project, accessExpression));
    }

    return ImmutableList.copyOf(eventDeclarationModels);
  }

  static EventDeclarationModel getEventDeclarationModel(
      Project project, PsiClassObjectAccessExpression psiExpression) {
    PsiType psiType = psiExpression.getType();

    final String text;
    if (psiType instanceof PsiClassType) {
      text = ((PsiClassType) psiType).getParameters()[0].getCanonicalText();
    } else {
      text = psiType.getCanonicalText();
    }

    PsiClass eventClass = PsiSearchUtils.findClass(project, text);
    if (eventClass == null) {
      throw new RuntimeException("Annotation class not found, text is: " + text);
    }

    return new EventDeclarationModel(
        PsiTypeUtils.guessClassName(text),
        getReturnType(eventClass),
        getFields(eventClass),
        psiType);
  }

  @Nullable
  static TypeName getReturnType(PsiClass psiClass) {
    PsiAnnotation eventAnnotation =
        AnnotationUtil.findAnnotation(psiClass, "com.facebook.litho.annotations.Event");
    PsiNameValuePair returnTypePair =
        AnnotationUtil.findDeclaredAttribute(eventAnnotation, "returnType");

    if (returnTypePair == null) {
      return TypeName.VOID;
    }

    PsiClassObjectAccessExpression returnTypeClassExpression =
        (PsiClassObjectAccessExpression) returnTypePair.getValue();
    PsiType returnTypeType = returnTypeClassExpression.getOperand().getType();

    return PsiTypeUtils.getTypeName(returnTypeType);
  }

  static ImmutableList<FieldModel> getFields(PsiClass psiClass) {
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
