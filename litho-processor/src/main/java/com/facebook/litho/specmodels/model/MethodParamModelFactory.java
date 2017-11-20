/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.ExecutableElement;

/**
 * Factory for creating {@link MethodParamModel}s.
 */
public final class MethodParamModelFactory {

  public static MethodParamModel create(
      ExecutableElement method,
      TypeName typeName,
      String name,
      List<Annotation> annotations,
      List<AnnotationSpec> externalAnnotations,
      List<Class<? extends Annotation>> permittedInterStateInputAnnotations,
      List<Class<? extends Annotation>> delegateMethodAnnotationsThatSkipDiffModels,
      Object representedObject) {

    // We check whether we're calling ShouldUpdate here since it uses a different infrastructure to
    // track previous props/state :(
    if (shouldCreateDiffModel(method, typeName, delegateMethodAnnotationsThatSkipDiffModels)) {
      return new RenderDataDiffModel(
          new SimpleMethodParamModel(
              typeName, name, annotations, externalAnnotations, representedObject));
    }

    final SimpleMethodParamModel simpleMethodParamModel =
        new SimpleMethodParamModel(
            extractDiffTypeIfNecessary(typeName),
            name,
            annotations,
            externalAnnotations,
            representedObject);

    for (Annotation annotation : annotations) {
      if (annotation instanceof Prop) {
        final PropModel propModel =
            new PropModel(
                simpleMethodParamModel,
                ((Prop) annotation).optional(),
                ((Prop) annotation).resType(),
                ((Prop) annotation).varArg());

        if (isDiffType(typeName)) {
          return new DiffPropModel(propModel);
        } else {
          return propModel;
        }
      }

      if (annotation instanceof State) {
        StateParamModel stateParamModel =
            new StateParamModel(simpleMethodParamModel, ((State) annotation).canUpdateLazily());

        if (isDiffType(typeName)) {
          return new DiffStateParamModel(stateParamModel);
        } else {
          return stateParamModel;
        }
      }

      if (annotation instanceof TreeProp) {
        return new TreePropModel(simpleMethodParamModel);
      }

      if (permittedInterStateInputAnnotations.contains(annotation.annotationType())) {
        return new InterStageInputParamModel(simpleMethodParamModel);
      }
    }

    return simpleMethodParamModel;
  }

  private static boolean shouldCreateDiffModel(
      ExecutableElement method,
      TypeName typeName,
      List<Class<? extends Annotation>> delegateMethodAnnotationsThatSkipDiffModels) {

    for (Class<? extends Annotation> delegate : delegateMethodAnnotationsThatSkipDiffModels) {
      if (method.getAnnotation(delegate) != null) {
        return false;
      }
    }

    return isDiffType(typeName);
  }

  private static boolean isDiffType(TypeName typeName) {
    return (typeName instanceof ParameterizedTypeName
        && ((ParameterizedTypeName) typeName).rawType.equals(ClassNames.DIFF));
  }


  static TypeName extractDiffTypeIfNecessary(TypeName typeName) {
    if (!isDiffType(typeName)) {
      return typeName;
    }

    return ((ParameterizedTypeName) typeName).typeArguments.get(0);
  }

  public static SimpleMethodParamModel createSimpleMethodParamModel(
      TypeName type, String name, Object representedObject) {
    return new SimpleMethodParamModel(
        type,
        name,
        ImmutableList.<Annotation>of(),
        ImmutableList.<AnnotationSpec>of(),
        representedObject);
  }
}
