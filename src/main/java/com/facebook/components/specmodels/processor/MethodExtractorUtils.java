// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.processor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.facebook.components.specmodels.model.MethodParamModel;
import com.facebook.components.specmodels.model.MethodParamModelFactory;

import com.squareup.javapoet.TypeName;

/**
 * Extracts event methods from the given input.
 */
public class MethodExtractorUtils {

  /**
   * @return a list of params for a method.
   */
  static List<MethodParamModel> getMethodParams(
      ExecutableElement method,
      List<Class<? extends Annotation>> permittedAnnotations) {
    final List<MethodParamModel> methodParams = new ArrayList<>();
    for (VariableElement param : method.getParameters()) {
      methodParams.add(
          MethodParamModelFactory.create(
              TypeName.get(param.asType()),
              param.getSimpleName().toString(),
              getParamAnnotations(param, permittedAnnotations),
              param));
    }

    return methodParams;
  }

  private static List<Annotation> getParamAnnotations(
      VariableElement param,
      List<Class<? extends Annotation>> permittedAnnotations) {
    List<Annotation> paramAnnotations = new ArrayList<>();
    for (Class<? extends Annotation> possibleMethodParamAnnotation : permittedAnnotations) {
      final Annotation paramAnnotation = param.getAnnotation(possibleMethodParamAnnotation);
      if (paramAnnotation != null) {
        paramAnnotations.add(paramAnnotation);
      }
    }

    return paramAnnotations;
  }
}
