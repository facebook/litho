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

/** Useful methods for {@link MethodParamModel}s. */
public class MethodParamModelUtils {

  public static boolean isAnnotatedWith(
      MethodParamModel methodParamModel, Class<? extends Annotation> annotationClass) {
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

  public static boolean isLazyStateParam(MethodParamModel methodParamModel) {
    return methodParamModel instanceof StateParamModel
        && ((StateParamModel) methodParamModel).canUpdateLazily();
  }

  public static boolean isComponentContextParam(MethodParamModel methodParamModel) {
    return methodParamModel.getTypeName().equals(ClassNames.COMPONENT_CONTEXT);
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
            .thenComparing(
                (a, b) ->
                    (a.getTypeName() == null && a.getTypeName() == b.getTypeName())
                            || a.getTypeName().equals(b.getTypeName())
                        ? 0
                        : -1)
            .thenComparing((a, b) -> a.getAnnotations().equals(b.getAnnotations()) ? 0 : -1));
  }
}
