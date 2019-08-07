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

import static com.facebook.litho.specmodels.processor.DelegateMethodExtractor.getPermittedMethodParamAnnotations;
import static com.facebook.litho.specmodels.processor.PsiEventDeclarationsExtractor.getFields;
import static com.facebook.litho.specmodels.processor.PsiEventDeclarationsExtractor.getReturnType;
import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getMethodParams;
import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getTypeVariables;

import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class PsiTriggerMethodExtractor {

  public static ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>>
      getOnTriggerMethods(
          PsiClass psiClass,
          List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<SpecMethodModel<EventMethod, EventDeclarationModel>> delegateMethods =
        new ArrayList<>();

    for (PsiMethod psiMethod : psiClass.getMethods()) {
      final OnTrigger onTriggerAnnotation =
          AnnotationUtil.findAnnotationInHierarchy(psiMethod, OnTrigger.class);
      if (onTriggerAnnotation != null) {
        final List<MethodParamModel> methodParams =
            getMethodParams(
                psiMethod,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                ImmutableList.<Class<? extends Annotation>>of());

        PsiAnnotation psiOnTriggerAnnotation =
            AnnotationUtil.findAnnotation(psiMethod, OnTrigger.class.getName());
        PsiNameValuePair valuePair =
            AnnotationUtil.findDeclaredAttribute(psiOnTriggerAnnotation, "value");
        PsiClassObjectAccessExpression valueClassExpression =
            (PsiClassObjectAccessExpression) valuePair.getValue();
        PsiType valueType = valueClassExpression.getOperand().getType();
        PsiClass valueClass = PsiTypesUtil.getPsiClass(valueType);

        // Reuse EventMethodModel and EventDeclarationModel because we are capturing the same info
        final SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod =
            new SpecMethodModel<EventMethod, EventDeclarationModel>(
                ImmutableList.<Annotation>of(),
                PsiProcessingUtils.extractModifiers(psiMethod.getModifierList()),
                psiMethod.getName(),
                null, // TypeName.get(psiMethod.getReturnType()),
                ImmutableList.copyOf(getTypeVariables(psiMethod)),
                ImmutableList.copyOf(methodParams),
                psiMethod,
                new EventDeclarationModel(
                    PsiTypeUtils.guessClassName(valueClass.getName()),
                    getReturnType(valueClass),
                    getFields(valueClass), // TODO fields in the Event class
                    valueClass));
        delegateMethods.add(eventMethod);
      }
    }

    return ImmutableList.copyOf(delegateMethods);
  }
}
