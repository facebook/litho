/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.annotation.concurrent.Immutable;

/**
 * Model that is an abstract representation of a Diff parameter to a lifecycle method. This is used
 * to supply the current and previous versions of a @Prop/@State parameter to a component lifecycle
 * method in a Diff object.
 */
@Immutable
public class DiffModel implements MethodParamModel {

  private final MethodParamModel mParamModel;
  private final boolean mNeedsRenderInfoInfra;

  DiffModel(MethodParamModel paramModel, boolean needsRenderInfoInfra) {
    mParamModel = paramModel;
    mNeedsRenderInfoInfra = needsRenderInfoInfra;
  }

  @Override
  public TypeName getType() {
    return mParamModel.getType();
  }

  @Override
  public String getName() {
    return mParamModel.getName();
  }

  public boolean needsRenderInfoInfra() {
    return mNeedsRenderInfoInfra;
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
    if (o instanceof DiffModel) {
      final DiffModel p = (DiffModel) o;
      return mParamModel.equals(p.mParamModel);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mParamModel.hashCode();
  }
}
