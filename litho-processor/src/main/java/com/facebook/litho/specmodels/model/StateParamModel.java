/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import javax.annotation.concurrent.Immutable;

import java.lang.annotation.Annotation;
import java.util.List;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.State}.
 */
@Immutable
public class StateParamModel implements MethodParamModel {
  private final MethodParamModel mParamModel;
  private final boolean mCanUpdateLazily;

  StateParamModel(MethodParamModel paramModel, boolean canUpdateLazily) {
    mParamModel = paramModel;
    mCanUpdateLazily = canUpdateLazily;
  }

  @Override
  public TypeName getType() {
    return mParamModel.getType();
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
  public List<AnnotationSpec> getExternalAnnotations() {
    return mParamModel.getExternalAnnotations();
  }

  @Override
  public Object getRepresentedObject() {
    return mParamModel.getRepresentedObject();
  }

  public boolean canUpdateLazily() {
    return mCanUpdateLazily;
  }

  public boolean equals(Object o) {
    if (o instanceof StateParamModel) {
      final StateParamModel p = (StateParamModel) o;
      return mParamModel.equals(p.mParamModel) && mCanUpdateLazily == p.mCanUpdateLazily;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mParamModel.hashCode() + (mCanUpdateLazily ? 1 : 0);
  }
}
