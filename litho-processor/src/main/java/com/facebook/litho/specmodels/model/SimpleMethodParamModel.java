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
import javax.annotation.concurrent.Immutable;

/**
 * Model that is a simple base representation of a method param.
 */
@Immutable
public class SimpleMethodParamModel implements MethodParamModel {
  private final TypeName mTypeName;
  private final String mName;
  private final List<Annotation> mAnnotations;
  private final List<AnnotationSpec> mExternalAnnotations;
  private final Object mRepresentedObject;

  SimpleMethodParamModel(
      TypeName typeName,
      String name,
      List<Annotation> annotations,
      List<AnnotationSpec> externalAnnotations,
      Object representedObject) {
    mTypeName = typeName;
    mName = name;
    mAnnotations = annotations;
    mExternalAnnotations = externalAnnotations;
    mRepresentedObject = representedObject;
  }

  @Override
  public TypeName getType() {
    return mTypeName;
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
    if (o instanceof SimpleMethodParamModel) {
      final SimpleMethodParamModel p = (SimpleMethodParamModel) o;
      return mTypeName.equals(p.mTypeName)
          && mName.equals(p.mName)
          && mAnnotations.equals(p.mAnnotations)
          && mExternalAnnotations.equals(p.mExternalAnnotations)
          && mRepresentedObject.equals(p.mRepresentedObject);
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = mTypeName.hashCode();
    result = 31 * result + mName.hashCode();
    result = 31 * result + mAnnotations.hashCode();
    result = 31 * result + mExternalAnnotations.hashCode();
    result = 31 * result + mRepresentedObject.hashCode();
    return result;
  }
}
