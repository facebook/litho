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
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * Model that is a simple base representation of a method param.
 */
@Immutable
public class SimpleMethodParamModel implements MethodParamModel {
  private final TypeSpec mTypeSpec;
  private final String mName;
  private final List<Annotation> mAnnotations;
  private final List<AnnotationSpec> mExternalAnnotations;
  private final Object mRepresentedObject;

  SimpleMethodParamModel(
      TypeSpec typeSpec,
      String name,
      List<Annotation> annotations,
      List<AnnotationSpec> externalAnnotations,
      Object representedObject) {
    mTypeSpec = typeSpec;
    mName = name;
    mAnnotations = annotations;
    mExternalAnnotations = externalAnnotations;
    mRepresentedObject = representedObject;
  }

  @Override
  public TypeSpec getTypeSpec() {
    return mTypeSpec;
  }

  @Override
  public TypeName getTypeName() {
    return mTypeSpec.getTypeName();
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final SimpleMethodParamModel that = (SimpleMethodParamModel) o;
    return Objects.equals(mTypeSpec, that.mTypeSpec)
        && Objects.equals(mName, that.mName)
        && Objects.equals(mAnnotations, that.mAnnotations)
        && Objects.equals(mExternalAnnotations, that.mExternalAnnotations)
        && Objects.equals(mRepresentedObject, that.mRepresentedObject);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mTypeSpec, mName, mAnnotations, mExternalAnnotations, mRepresentedObject);
  }
}
