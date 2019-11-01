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
import com.facebook.litho.specmodels.model.WorkingRangeDeclarationModel;
import com.facebook.litho.specmodels.model.WorkingRangeMethodModel;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

class PsiWorkingRangesMethodExtractor {

  @Nullable
  static SpecMethodModel<EventMethod, Void> getRegisterMethod(
      PsiClass psiClass, List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    for (PsiMethod psiMethod : psiClass.getMethods()) {
      final OnRegisterRanges onRegisterRangesAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiMethod, OnRegisterRanges.class);
      if (onRegisterRangesAnnotation != null) {
        final List<MethodParamModel> methodParams =
            getMethodParams(
                psiMethod,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                ImmutableList.of());

        return SpecMethodModel.<EventMethod, Void>builder()
            .annotations(ImmutableList.of())
            .modifiers(PsiModifierExtractor.extractModifiers(psiMethod.getModifierList()))
            .name(psiMethod.getName())
            .returnTypeSpec(PsiTypeUtils.generateTypeSpec(psiMethod.getReturnType()))
            .typeVariables(ImmutableList.copyOf(getTypeVariables(psiMethod)))
            .methodParams(ImmutableList.copyOf(methodParams))
            .representedObject(psiMethod)
            .build();
      }
    }
    return null;
  }

  static ImmutableList<WorkingRangeMethodModel> getRangesMethods(
      PsiClass psiClass, List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    Map<String, WorkingRangeMethodModel> models = new HashMap<>();

    for (PsiMethod psiMethod : psiClass.getMethods()) {
      // OnEnteredRange
      final OnEnteredRange enteredRangeAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiMethod, OnEnteredRange.class);
      if (enteredRangeAnnotation != null) {
        SpecMethodModel<EventMethod, WorkingRangeDeclarationModel> enteredRangeMethod =
            generateWorkingRangeMethod(
                psiMethod, permittedInterStageInputAnnotations, OnEnteredRange.class.getName());
        final String name = enteredRangeAnnotation.name();
        models.putIfAbsent(name, new WorkingRangeMethodModel(name));
        models.get(name).enteredRangeModel = enteredRangeMethod;
      }

      // OnExitedRange
      final OnExitedRange exitedRangeAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiMethod, OnExitedRange.class);
      if (exitedRangeAnnotation != null) {
        SpecMethodModel<EventMethod, WorkingRangeDeclarationModel> exitedRangeMethod =
            generateWorkingRangeMethod(
                psiMethod, permittedInterStageInputAnnotations, OnExitedRange.class.getName());
        final String name = exitedRangeAnnotation.name();
        models.putIfAbsent(name, new WorkingRangeMethodModel(name));
        models.get(name).exitedRangeModel = exitedRangeMethod;
      }
    }
    return ImmutableList.copyOf(new ArrayList<>(models.values()));
  }

  private static SpecMethodModel<EventMethod, WorkingRangeDeclarationModel>
      generateWorkingRangeMethod(
          PsiMethod psiMethod,
          List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
          String annotationQualifiedName) {
    final List<MethodParamModel> methodParams =
        getMethodParams(
            psiMethod,
            getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
            permittedInterStageInputAnnotations,
            ImmutableList.of());

    PsiAnnotation psiAnnotation = AnnotationUtil.findAnnotation(psiMethod, annotationQualifiedName);
    PsiNameValuePair valuePair = AnnotationUtil.findDeclaredAttribute(psiAnnotation, "name");

    return SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
        .annotations(ImmutableList.of())
        .modifiers(PsiModifierExtractor.extractModifiers(psiMethod.getModifierList()))
        .name(psiMethod.getName())
        .returnTypeSpec(PsiTypeUtils.generateTypeSpec(psiMethod.getReturnType()))
        .typeVariables(ImmutableList.copyOf(getTypeVariables(psiMethod)))
        .methodParams(ImmutableList.copyOf(methodParams))
        .representedObject(psiMethod)
        .typeModel(
            new WorkingRangeDeclarationModel(
                valuePair.getLiteralValue(), valuePair.getNameIdentifier()))
        .build();
  }
}
