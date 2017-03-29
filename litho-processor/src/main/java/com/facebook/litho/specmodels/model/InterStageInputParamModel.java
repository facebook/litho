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
  public List<AnnotationSpec> getExternalAnnotations() {
    return mParamModel.getExternalAnnotations();
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
