/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.ResType;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.InjectProp}.
 */
@Immutable
public class InjectPropModel implements MethodParamModel {
  private final MethodParamModel mParamModel;

  public InjectPropModel(MethodParamModel paramModel) {
    mParamModel = paramModel;
  }

  @Override
  public TypeSpec getTypeSpec() {
    return mParamModel.getTypeSpec();
  }

  @Override
  public TypeName getTypeName() {
    return mParamModel.getTypeName();
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

  @Override
  public boolean equals(Object o) {
    if (o instanceof InjectPropModel) {
      final InjectPropModel p = (InjectPropModel) o;
      return mParamModel.equals(p.mParamModel);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mParamModel.hashCode();
  }

  /** Convert to a regular prop model. */
  public PropModel toPropModel() {
    return new PropModel(mParamModel, false, ResType.NONE, "");
  }
}
