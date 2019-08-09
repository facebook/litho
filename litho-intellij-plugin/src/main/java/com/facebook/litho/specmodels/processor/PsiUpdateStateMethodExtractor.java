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

import static com.facebook.litho.specmodels.internal.ImmutableList.copyOf;
import static com.facebook.litho.specmodels.processor.DelegateMethodExtractor.getPermittedMethodParamAnnotations;
import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getMethodParams;

import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.UpdateStateMethod;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.squareup.javapoet.TypeVariableName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class PsiUpdateStateMethodExtractor {

  public static ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> getOnUpdateStateMethods(
      PsiClass psiClass,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
      boolean isTransition) {
    final List<SpecMethodModel<UpdateStateMethod, Void>> delegateMethods = new ArrayList<>();

    for (PsiMethod psiMethod : psiClass.getMethods()) {
      final Annotation onUpdateStateAnnotation =
          isTransition
              ? PsiAnnotationProxyUtils.findAnnotationInHierarchy(
                  psiMethod, OnUpdateStateWithTransition.class)
              : PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiMethod, OnUpdateState.class);

      if (onUpdateStateAnnotation != null) {
        final List<MethodParamModel> methodParams =
            getMethodParams(
                psiMethod,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                ImmutableList.<Class<? extends Annotation>>of());

        final SpecMethodModel<UpdateStateMethod, Void> delegateMethod =
            SpecMethodModel.<UpdateStateMethod, Void>builder()
                .annotations(ImmutableList.<Annotation>of(onUpdateStateAnnotation))
                .modifiers(PsiModifierExtractor.extractModifiers(psiMethod.getModifierList()))
                .name(psiMethod.getName())
                .returnTypeSpec(PsiTypeUtils.generateTypeSpec(psiMethod.getReturnType()))
                .typeVariables(ImmutableList.<TypeVariableName>of())
                .methodParams(copyOf(methodParams))
                .representedObject(psiMethod)
                .build();
        delegateMethods.add(delegateMethod);
      }
    }

    return copyOf(delegateMethods);
  }
}
