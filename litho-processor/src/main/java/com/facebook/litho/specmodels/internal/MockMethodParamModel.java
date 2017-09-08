/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.internal;

import com.facebook.litho.specmodels.model.MethodParamModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/** A simple implementation of {@link MockMethodParamModel} for use in tests. */
@Immutable
public class MockMethodParamModel implements MethodParamModel {
  private final TypeName mType;
  private final String mName;
  private final List<Annotation> mAnnotations;
  private final List<AnnotationSpec> mExternalAnnotations;
  private final Object mRepresentedObject;

  public MockMethodParamModel(
      TypeName type,
      String name,
      List<Annotation> annotations,
      List<AnnotationSpec> externalAnnotations,
      Object representedObject) {
    mType = type;
    mName = name;
    mAnnotations = annotations;
    mExternalAnnotations = externalAnnotations;
    mRepresentedObject = representedObject;
  }

  @Override
  public TypeName getType() {
    return mType;
  }

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public List<Annotation> getAnnotations() {
    return mAnnotations;
  }

  @Override
  public List<AnnotationSpec> getExternalAnnotations() {
    return mExternalAnnotations;
  }

  @Override
  public Object getRepresentedObject() {
    return mRepresentedObject;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private TypeName mType;
    private String mName;
    private List<Annotation> mAnnotations = ImmutableList.of();
    private List<AnnotationSpec> mExternalAnnotations = ImmutableList.of();
    private Object mRepresentedObject;

    public Builder type(TypeName type) {
      mType = type;
      return this;
    }

    public Builder name(String name) {
      mName = name;
      return this;
    }

    public Builder annotations(List<Annotation> annotations) {
      mAnnotations = annotations;
      return this;
    }

    public Builder externalAnnotations(List<AnnotationSpec> externalAnnotations) {
      mExternalAnnotations = externalAnnotations;
      return this;
    }

    public Builder representedObject(Object representedObject) {
      mRepresentedObject = representedObject;
      return this;
    }

    public MockMethodParamModel build() {
      return new MockMethodParamModel(
          mType, mName, mAnnotations, mExternalAnnotations, mRepresentedObject);
    }
  }
}
