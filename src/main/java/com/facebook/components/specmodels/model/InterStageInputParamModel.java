// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.annotation.concurrent.Immutable;

import java.lang.annotation.Annotation;
import java.util.List;

import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a method param that is an inter-stage input.
 */
@Immutable
public class InterStageInputParamModel implements MethodParamModel {
  private final MethodParamModel mParamModel;

  InterStageInputParamModel(MethodParamModel paramModel) {
    mParamModel = paramModel;
  }

  @Override
  public TypeName getType() {
    return mParamModel.getType().box();
  }

  @Override
  public String getName() {
    return mParamModel.getName();
  }

  @Override
  public List<Annotation> getAnnotations() {
    return mParamModel.getAnnotations();
  }

  @Override
  public Object getRepresentedObject() {
    return mParamModel.getRepresentedObject();
  }

  public boolean equals(Object o) {
    if (o instanceof InterStageInputParamModel) {
      final InterStageInputParamModel p = (InterStageInputParamModel) o;
      return mParamModel.equals(p.mParamModel);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mParamModel.hashCode();
  }
}
