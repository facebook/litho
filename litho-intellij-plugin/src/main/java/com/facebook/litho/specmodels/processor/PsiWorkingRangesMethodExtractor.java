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
import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getMethodParams;
import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getTypeVariables;

import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnExitedRange;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.TypeSpec;
import com.facebook.litho.specmodels.model.WorkingRangeDeclarationModel;
import com.facebook.litho.specmodels.model.WorkingRangeMethodModel;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class PsiWorkingRangesMethodExtractor {

  @Nullable
  public static SpecMethodModel<EventMethod, Void> getRegisterMethod(
      PsiClass psiClass, List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    for (PsiMethod psiMethod : psiClass.getMethods()) {
      final OnRegisterRanges onRegisterRangesAnnotation =
          AnnotationUtil.findAnnotationInHierarchy(psiMethod, OnRegisterRanges.class);
      if (onRegisterRangesAnnotation != null) {
        final List<MethodParamModel> methodParams =
            getMethodParams(
                psiMethod,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                ImmutableList.<Class<? extends Annotation>>of());

        return SpecMethodModel.<EventMethod, Void>builder()
            .annotations(ImmutableList.of())
            .modifiers(PsiProcessingUtils.extractModifiers(psiMethod.getModifierList()))
            .name(psiMethod.getName())
            .returnTypeSpec(new TypeSpec(TypeName.VOID))
            .typeVariables(ImmutableList.copyOf(getTypeVariables(psiMethod)))
            .methodParams(ImmutableList.copyOf(methodParams))
            .representedObject(psiMethod)
            .build();
      }
    }
    return null;
  }

  public static ImmutableList<WorkingRangeMethodModel> getRangesMethods(
      PsiClass psiClass, List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<WorkingRangeMethodModel> workingRangeMethods = new ArrayList<>();

    for (PsiMethod psiMethod : psiClass.getMethods()) {
      final OnEnteredRange enteredRangeAnnotation =
          AnnotationUtil.findAnnotationInHierarchy(psiMethod, OnEnteredRange.class);
      final OnExitedRange exitedRangeAnnotation =
          AnnotationUtil.findAnnotationInHierarchy(psiMethod, OnExitedRange.class);

      if (enteredRangeAnnotation != null) {
        SpecMethodModel<EventMethod, WorkingRangeDeclarationModel> enteredRangeMethod =
            generateWorkingRangeMethod(
                psiMethod, permittedInterStageInputAnnotations, "OnEnteredRange");

        final String name = enteredRangeAnnotation.name();
        final WorkingRangeMethodModel workingRangeModel =
            workingRangeMethods.stream()
                .filter(it -> it.name.equals(name) && it.enteredRangeModel == null)
                .findFirst()
                .orElseGet(
                    () -> {
                      WorkingRangeMethodModel model = new WorkingRangeMethodModel(name);
                      workingRangeMethods.add(model);
                      return model;
                    });
        workingRangeModel.enteredRangeModel = enteredRangeMethod;
      }

      if (exitedRangeAnnotation != null) {
        SpecMethodModel<EventMethod, WorkingRangeDeclarationModel> exitedRangeMethod =
            generateWorkingRangeMethod(
                psiMethod, permittedInterStageInputAnnotations, "OnExitedRange");

        final String name = exitedRangeAnnotation.name();
        final WorkingRangeMethodModel workingRangeModel =
            workingRangeMethods.stream()
                .filter(it -> it.name.equals(name) && it.exitedRangeModel == null)
                .findFirst()
                .orElseGet(
                    () -> {
                      WorkingRangeMethodModel model = new WorkingRangeMethodModel(name);
                      workingRangeMethods.add(model);
                      return model;
                    });
        workingRangeModel.exitedRangeModel = exitedRangeMethod;
      }
    }
    return ImmutableList.copyOf(workingRangeMethods);
  }

  @Nullable
  private static SpecMethodModel<EventMethod, WorkingRangeDeclarationModel>
      generateWorkingRangeMethod(
          PsiMethod psiMethod,
          List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
          String annotation) {
    final List<MethodParamModel> methodParams =
        getMethodParams(
            psiMethod,
            getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
            permittedInterStageInputAnnotations,
            ImmutableList.<Class<? extends Annotation>>of());

    PsiAnnotation psiOnEnteredRangeAnnotation =
        AnnotationUtil.findAnnotation(psiMethod, annotation);
    PsiNameValuePair valuePair =
        AnnotationUtil.findDeclaredAttribute(psiOnEnteredRangeAnnotation, "name");
    PsiClassObjectAccessExpression valueClassExpression =
        (PsiClassObjectAccessExpression) valuePair.getValue();
    PsiType valueType = valueClassExpression.getOperand().getType();
    PsiClass valueClass = PsiTypesUtil.getPsiClass(valueType);

    return SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
        .annotations(ImmutableList.of())
        .modifiers(PsiProcessingUtils.extractModifiers(psiMethod.getModifierList()))
        .name(psiMethod.getName())
        .returnTypeSpec(new TypeSpec(TypeName.VOID))
        .typeVariables(ImmutableList.copyOf(getTypeVariables(psiMethod)))
        .methodParams(ImmutableList.copyOf(methodParams))
        .representedObject(psiMethod)
        .typeModel(new WorkingRangeDeclarationModel(valuePair.getLiteralValue(), valueClass))
        .build();
  }
}
