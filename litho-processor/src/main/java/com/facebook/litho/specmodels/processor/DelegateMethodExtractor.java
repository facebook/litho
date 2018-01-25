/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.model.SpecModelUtils.generateTypeSpec;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getMethodParams;

import com.facebook.litho.annotations.InjectProp;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

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
    METHOD_PARAM_ANNOTATIONS.add(InjectProp.class);
  }

  /** Get the delegate methods from the given {@link TypeElement}. */
  public static ImmutableList<SpecMethodModel<DelegateMethod, Void>> getDelegateMethods(
      TypeElement typeElement,
      List<Class<? extends Annotation>> permittedMethodAnnotations,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
      List<Class<? extends Annotation>> delegateMethodAnnotationsThatSkipDiffModels,
      Messager messager) {
    final List<SpecMethodModel<DelegateMethod, Void>> delegateMethods = new ArrayList<>();

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
                messager,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                delegateMethodAnnotationsThatSkipDiffModels);

        final SpecMethodModel<DelegateMethod, Void> delegateMethod =
            new SpecMethodModel<DelegateMethod, Void>(
                ImmutableList.<Annotation>copyOf(methodAnnotations),
                ImmutableList.<Modifier>copyOf(new ArrayList<>(executableElement.getModifiers())),
                executableElement.getSimpleName(),
                generateTypeSpec(executableElement.getReturnType()),
                ImmutableList.of(),
                ImmutableList.copyOf(methodParams),
                enclosedElement,
                null);
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

  static List<Class<? extends Annotation>> getPermittedMethodParamAnnotations(
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<Class<? extends Annotation>> permittedMethodParamAnnotations =
        new ArrayList<>(METHOD_PARAM_ANNOTATIONS);
    permittedMethodParamAnnotations.addAll(permittedInterStageInputAnnotations);

    return permittedMethodParamAnnotations;
  }
}
