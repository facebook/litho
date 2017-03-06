// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

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

  public static List<TypeVariableName> getTypeVariables(MethodParamModel methodParam) {
    return getTypeVariables(methodParam.getType());
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
}
