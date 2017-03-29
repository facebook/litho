/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;

/**
 * Factory for creating {@link MethodParamModel}s.
 */
public final class MethodParamModelFactory {

  public static MethodParamModel create(
      TypeName type,
      String name,
      List<Annotation> annotations,
      List<AnnotationSpec> externalAnnotations,
      List<Class<? extends Annotation>> permittedInterStateInputAnnotations,
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

      if (permittedInterStateInputAnnotations.contains(annotation.annotationType())) {
        return new InterStageInputParamModel(simpleMethodParamModel);
      }
    }

    return simpleMethodParamModel;
  }
}
