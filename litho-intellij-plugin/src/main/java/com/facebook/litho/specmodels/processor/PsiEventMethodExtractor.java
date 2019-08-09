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

import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getMethodParams;
import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getTypeVariables;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiMethod;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/** Extracts event methods from the given input. */
public class PsiEventMethodExtractor {

  public static ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>>
      getOnEventMethods(
          Project project,
          PsiClass psiClass,
          List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<SpecMethodModel<EventMethod, EventDeclarationModel>> delegateMethods =
        new ArrayList<>();

    for (PsiMethod psiMethod : psiClass.getMethods()) {
      final PsiAnnotation onEventAnnotation =
          AnnotationUtil.findAnnotation(psiMethod, "com.facebook.litho.annotations.OnEvent");
      if (onEventAnnotation != null) {
        final List<MethodParamModel> methodParams =
            getMethodParams(
                psiMethod,
                EventMethodExtractor.getPermittedMethodParamAnnotations(
                    permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                ImmutableList.<Class<? extends Annotation>>of());

        PsiAnnotationMemberValue psiAnnotationMemberValue =
            onEventAnnotation.findAttributeValue("value");

        PsiClassObjectAccessExpression accessExpression =
            (PsiClassObjectAccessExpression) psiAnnotationMemberValue;

        final SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod =
            new SpecMethodModel<>(
                ImmutableList.<Annotation>of(),
                PsiModifierExtractor.extractModifiers(psiMethod.getModifierList()),
                psiMethod.getName(),
                PsiTypeUtils.generateTypeSpec(psiMethod.getReturnType()),
                ImmutableList.copyOf(getTypeVariables(psiMethod)),
                ImmutableList.copyOf(methodParams),
                psiMethod,
                PsiEventDeclarationsExtractor.getEventDeclarationModel(project, accessExpression));
        delegateMethods.add(eventMethod);
      }
    }

    return ImmutableList.copyOf(delegateMethods);
  }
}
