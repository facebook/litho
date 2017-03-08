// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.facebook.components.specmodels.model.MethodParamModel;
import com.facebook.components.specmodels.model.MethodParamModelFactory;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

/**
 * Extracts methods from the given input.
 */
public class MethodExtractorUtils {
  private static final String COMPONENTS_PACKAGE = "com.facebook.components";

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
              getLibraryAnnotations(param, permittedAnnotations),
              getExternalAnnotations(param),
              param));
    }

    return methodParams;
  }

  private static List<Annotation> getLibraryAnnotations(
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

  private static List<AnnotationSpec> getExternalAnnotations(VariableElement param) {
    final List<? extends AnnotationMirror> annotationMirrors = param.getAnnotationMirrors();
    final List<AnnotationSpec> annotations = new ArrayList<>();

    for (AnnotationMirror annotationMirror : annotationMirrors) {
      if (annotationMirror.getAnnotationType().toString().startsWith(COMPONENTS_PACKAGE)) {
        continue;
      }

      final AnnotationSpec.Builder annotationSpec =
          AnnotationSpec.builder(
              ClassName.bestGuess(annotationMirror.getAnnotationType().toString()));

      Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
          annotationMirror.getElementValues();
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> elementValue :
          elementValues.entrySet()) {
        annotationSpec.addMember(
            elementValue.getKey().getSimpleName().toString(),
            elementValue.getValue().toString());
      }

      annotations.add(annotationSpec.build());
    }

    return annotations;
  }
}
