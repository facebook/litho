/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.MethodParamModel;

import com.squareup.javapoet.TypeName;

import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getMethodParams;

/**
 * Extracts delegate methods from the given input.
 */
public class DelegateMethodExtractor {

  private static final List<Class<? extends Annotation>> METHOD_PARAM_ANNOTATIONS =
      new ArrayList<>();
  static {
    METHOD_PARAM_ANNOTATIONS.add(Prop.class);
    METHOD_PARAM_ANNOTATIONS.add(State.class);
    METHOD_PARAM_ANNOTATIONS.add(TreeProp.class);
  }

  /**
   * Get the delegate methods from the given {@link TypeElement}.
   */
  public static ImmutableList<DelegateMethodModel> getDelegateMethods(
      TypeElement typeElement,
      List<Class<? extends Annotation>> permittedMethodAnnotations,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<DelegateMethodModel> delegateMethods = new ArrayList<>();

    for (Element enclosedElement : typeElement.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      final List<Annotation> methodAnnotations =
          getMethodAnnotations(enclosedElement, permittedMethodAnnotations);

      if (!methodAnnotations.isEmpty()) {
        final ExecutableElement executableElement = (ExecutableElement) enclosedElement;
        final List<MethodParamModel> methodParams =
            getMethodParams(
                executableElement,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations);

        final DelegateMethodModel delegateMethod =
            new DelegateMethodModel(
                ImmutableList.copyOf(methodAnnotations),
                ImmutableList.copyOf(new ArrayList<>(executableElement.getModifiers())),
                executableElement.getSimpleName(),
                TypeName.get(executableElement.getReturnType()),
                ImmutableList.copyOf(methodParams),
                enclosedElement);
        delegateMethods.add(delegateMethod);
      }
    }

    return ImmutableList.copyOf(delegateMethods);
  }

  private static List<Annotation> getMethodAnnotations(
      Element method,
      List<Class<? extends Annotation>> permittedMethodAnnotations) {
    List<Annotation> methodAnnotations = new ArrayList<>();
    for (Class<? extends Annotation> possibleDelegateMethodAnnotation :
        permittedMethodAnnotations) {
      final Annotation methodAnnotation =
          method.getAnnotation(possibleDelegateMethodAnnotation);
      if (methodAnnotation != null) {
        methodAnnotations.add(methodAnnotation);
      }
    }

    return methodAnnotations;
  }

  private static List<Class<? extends Annotation>> getPermittedMethodParamAnnotations(
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<Class<? extends Annotation>> permittedMethodParamAnnotations =
        new ArrayList<>(METHOD_PARAM_ANNOTATIONS);
    permittedMethodParamAnnotations.addAll(permittedInterStageInputAnnotations);

    return permittedMethodParamAnnotations;
  }
}
