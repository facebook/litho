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

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.ResType;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.Prop}.
 */
@Immutable
public class PropModel implements MethodParamModel {
  private final MethodParamModel mParamModel;
  private final boolean mIsOptional;
  private final ResType mResType;

  PropModel(
      MethodParamModel paramModel,
      boolean isOptional,
      ResType resType) {
    mParamModel = paramModel;
    mIsOptional = isOptional;
    mResType = resType;
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

  public boolean isOptional() {
    return mIsOptional;
  }

  public ResType getResType() {
    return mResType;
  }

  /**
   * @return true if this prop has a default specified in the given set of defaults, false
   * otherwise.
   */
  public boolean hasDefault(ImmutableList<PropDefaultModel> propDefaults) {
    for (PropDefaultModel propDefault : propDefaults) {
      if (propDefault.mType.equals(mParamModel.getType())
          && propDefault.mName.equals(mParamModel.getName())) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PropModel) {
      final PropModel p = (PropModel) o;
      return mParamModel.equals(p.mParamModel)
          && mIsOptional == p.mIsOptional
          && mResType.equals(p.mResType);
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = mParamModel.hashCode();
    result = 17 * result + (mIsOptional ? 1 : 0);
    result = 31 * result + mResType.hashCode();
    return result;
  }
}
