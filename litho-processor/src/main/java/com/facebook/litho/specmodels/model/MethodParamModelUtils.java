/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Useful methods for {@link MethodParamModel}s.
 */
public class MethodParamModelUtils {

  public static boolean isAnnotatedWith(
      MethodParamModel methodParamModel,
      Class<? extends Annotation> annotationClass) {
    for (Annotation annotation : methodParamModel.getAnnotations()) {
      if (annotation.annotationType().equals(annotationClass)) {
        return true;
      }
    }

    return false;
  }

  public static boolean isAnnotatedWithExternalAnnotation(
      MethodParamModel methodParamModel, TypeName annotationType) {
    for (AnnotationSpec annotation : methodParamModel.getExternalAnnotations()) {
      if (annotation.type.equals(annotationType)) {
        return true;
      }
    }

    return false;
  }

  public static @Nullable Annotation getAnnotation(
      MethodParamModel methodParamModel, Class<? extends Annotation> annotationClass) {
    for (Annotation annotation : methodParamModel.getAnnotations()) {
      if (annotation.annotationType().equals(annotationClass)) {
        return annotation;
      }
    }

    return null;
  }

  public static List<TypeVariableName> getTypeVariables(MethodParamModel methodParam) {
    return getTypeVariables(methodParam.getTypeName());
  }

  public static List<TypeVariableName> getTypeVariables(TypeName typeName) {
    final List<TypeVariableName> typeVariables = new ArrayList<>();

    if (typeName instanceof TypeVariableName) {
      typeVariables.add((TypeVariableName) typeName);
    } else if (typeName instanceof ParameterizedTypeName) {
      for (TypeName typeArgument : ((ParameterizedTypeName) typeName).typeArguments) {
        typeVariables.addAll(getTypeVariables(typeArgument));
      }
    }

    return typeVariables;
  }

  /** Compares two {@link MethodParamModel}s based on name and annotations only. */
  public static Comparator<MethodParamModel> shallowParamComparator() {
    return Comparator.nullsLast(
        Comparator.comparing(MethodParamModel::getName)
            .thenComparing((a, b) -> a.getAnnotations().equals(b.getAnnotations()) ? 0 : -1));
  }
}
