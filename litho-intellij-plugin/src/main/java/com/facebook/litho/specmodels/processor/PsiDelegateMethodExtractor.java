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

import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getMethodParams;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.squareup.javapoet.TypeVariableName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/** Extracts delegate methods from the given input. */
public class PsiDelegateMethodExtractor {

  public static ImmutableList<SpecMethodModel<DelegateMethod, Void>> getDelegateMethods(
      PsiClass psiClass,
      List<Class<? extends Annotation>> permittedMethodAnnotations,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
      List<Class<? extends Annotation>> delegateMethodAnnotationsThatSkipDiffModels) {
    final List<SpecMethodModel<DelegateMethod, Void>> delegateMethods = new ArrayList<>();

    for (PsiMethod psiMethod : psiClass.getMethods()) {
      final List<Annotation> methodAnnotations =
          getMethodAnnotations(psiMethod, permittedMethodAnnotations);

      if (!methodAnnotations.isEmpty()) {
        final List<MethodParamModel> methodParams =
            getMethodParams(
                psiMethod,
                DelegateMethodExtractor.getPermittedMethodParamAnnotations(
                    permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                delegateMethodAnnotationsThatSkipDiffModels);

        final SpecMethodModel<DelegateMethod, Void> delegateMethod =
            new SpecMethodModel<>(
                ImmutableList.copyOf(methodAnnotations),
                PsiModifierExtractor.extractModifiers(psiMethod.getModifierList()),
                psiMethod.getName(),
                PsiTypeUtils.generateTypeSpec(psiMethod.getReturnType()),
                ImmutableList.<TypeVariableName>of(),
                ImmutableList.copyOf(methodParams),
                psiMethod,
                null);
        delegateMethods.add(delegateMethod);
      }
    }

    return ImmutableList.copyOf(delegateMethods);
  }

  private static List<Annotation> getMethodAnnotations(
      PsiMethod psiMethod, List<Class<? extends Annotation>> permittedMethodAnnotations) {
    List<Annotation> methodAnnotations = new ArrayList<>();
    for (Class<? extends Annotation> possibleDelegateMethodAnnotation :
        permittedMethodAnnotations) {
      final Annotation methodAnnotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(
              psiMethod, possibleDelegateMethodAnnotation);
      if (methodAnnotation != null) {
        methodAnnotations.add(methodAnnotation);
      }
    }

    return methodAnnotations;
  }
}
