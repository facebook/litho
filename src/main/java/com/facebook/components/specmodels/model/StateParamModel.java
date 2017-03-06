// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.annotation.concurrent.Immutable;

import java.lang.annotation.Annotation;
import java.util.List;

import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a {@link com.facebook.components.annotations.State}.
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
