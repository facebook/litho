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
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.State} that
 * has type Diff.
 */
@Immutable
public class DiffStateParamModel implements MethodParamModel {
  private final StateParamModel mUnderlyingStateParamModel;

  DiffStateParamModel(StateParamModel stateParamModel) {
    mUnderlyingStateParamModel = stateParamModel;
  }

  @Override
  public TypeName getTypeName() {
    return ParameterizedTypeName.get(ClassNames.DIFF, mUnderlyingStateParamModel.getTypeName().box());
  }

  @Override
  public String getName() {
    return mUnderlyingStateParamModel.getName();
  }

  @Override
  public List<Annotation> getAnnotations() {
    return mUnderlyingStateParamModel.getAnnotations();
  }

  @Override
  public List<AnnotationSpec> getExternalAnnotations() {
    return mUnderlyingStateParamModel.getExternalAnnotations();
  }

  @Override
  public Object getRepresentedObject() {
    return mUnderlyingStateParamModel.getRepresentedObject();
  }

  public StateParamModel getUnderlyingStateParamModel() {
    return mUnderlyingStateParamModel;
  }

  public boolean canUpdateLazily() {
    return mUnderlyingStateParamModel.canUpdateLazily();
  }

  public boolean equals(Object o) {
    if (o instanceof DiffStateParamModel) {
      final DiffStateParamModel p = (DiffStateParamModel) o;
      return mUnderlyingStateParamModel.equals(p.mUnderlyingStateParamModel);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mUnderlyingStateParamModel.hashCode();
  }

  public boolean isSameUnderlyingStateValueModel(StateParamModel stateParamModel) {
    return stateParamModel.getName().equals(getName()) &&
        stateParamModel.getTypeName().box().equals(mUnderlyingStateParamModel.getTypeName().box()) &&
        stateParamModel.canUpdateLazily() == canUpdateLazily();
  }
}
