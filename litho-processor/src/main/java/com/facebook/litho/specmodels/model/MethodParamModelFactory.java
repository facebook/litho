/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import static com.facebook.litho.specmodels.model.ClassNames.DIFF;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Factory for creating {@link MethodParamModel}s.
 */
public final class MethodParamModelFactory {

  public static MethodParamModel create(
      TypeSpec typeSpec,
      String name,
      List<Annotation> annotations,
      List<AnnotationSpec> externalAnnotations,
      List<Class<? extends Annotation>> permittedInterStateInputAnnotations,
      boolean canCreateDiffModels,
      Object representedObject) {

    if (canCreateDiffModels && typeSpec.isSameDeclaredType(DIFF)) {
      return new RenderDataDiffModel(
          new SimpleMethodParamModel(
              typeSpec, name, annotations, externalAnnotations, representedObject));
    }

    final SimpleMethodParamModel simpleMethodParamModel =
        new SimpleMethodParamModel(
            extractDiffTypeIfNecessary(typeSpec),
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

        if (typeSpec.isSameDeclaredType(DIFF)) {
          return new DiffPropModel(propModel);
        } else {
          return propModel;
        }
      }

      if (annotation instanceof State) {
        StateParamModel stateParamModel =
            new StateParamModel(simpleMethodParamModel, ((State) annotation).canUpdateLazily());

        if (typeSpec.isSameDeclaredType(DIFF)) {
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

  static TypeSpec extractDiffTypeIfNecessary(TypeSpec typeSpec) {
    if (typeSpec.isSameDeclaredType(DIFF)) {
      return ((TypeSpec.DeclaredTypeSpec) typeSpec).getTypeArguments().get(0);
    }

    return typeSpec;
  }

  public static SimpleMethodParamModel createSimpleMethodParamModel(
      TypeSpec typeSpec, String name, Object representedObject) {
    return new SimpleMethodParamModel(
        typeSpec,
        name,
        ImmutableList.<Annotation>of(),
        ImmutableList.<AnnotationSpec>of(),
        representedObject);
  }
}
