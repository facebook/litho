// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.facebook.components.annotations.FromCreateLayout;
import com.facebook.components.annotations.FromMeasure;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.State;
import com.facebook.components.annotations.TreeProp;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;

/**
 * Factory for creating {@link MethodParamModel}s.
 */
public final class MethodParamModelFactory {
  private static final List<Class<? extends Annotation>> INTER_STAGE_INPUT_ANNOTATIONS =
      new ArrayList<>();
  static {
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromCreateLayout.class);
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromMeasure.class);
  }

  public static MethodParamModel create(
      TypeName type,
      String name,
      List<Annotation> annotations,
      List<AnnotationSpec> externalAnnotations,
      Object representedObject) {
    final SimpleMethodParamModel simpleMethodParamModel =
        new SimpleMethodParamModel(type, name, annotations, externalAnnotations, representedObject);
    for (Annotation annotation : annotations) {
      if (annotation instanceof Prop) {
        return new PropModel(
            simpleMethodParamModel,
            ((Prop) annotation).optional(),
            ((Prop) annotation).resType());
      }

      if (annotation instanceof State) {
        return new StateParamModel(simpleMethodParamModel, ((State) annotation).canUpdateLazily());
      }

      if (annotation instanceof TreeProp) {
        return new TreePropModel(simpleMethodParamModel);
      }

      if (INTER_STAGE_INPUT_ANNOTATIONS.contains(annotation.annotationType())) {
        return new InterStageInputParamModel(simpleMethodParamModel);
      }
    }

    return simpleMethodParamModel;
  }
}
