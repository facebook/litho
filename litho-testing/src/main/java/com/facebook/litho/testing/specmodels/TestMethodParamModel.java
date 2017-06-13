/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.specmodels;

import com.facebook.litho.specmodels.model.MethodParamModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class TestMethodParamModel implements MethodParamModel {

  private final List<Annotation> mAnnotations = new ArrayList<>();
  private final TypeName mType;
  private final String mName;
  private final Object mRepresentedObject;

  public TestMethodParamModel(
      TypeName type,
      String name,
      final Class<? extends Annotation> annotation,
      Object representedObject) {
    mType = type;
    mName = name;
    if (annotation != null) {
      mAnnotations.add(new Annotation() {
        @Override
        public Class<? extends Annotation> annotationType() {
          return annotation;
        }
      });
    }
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
    return new ArrayList<>();
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
    private Class<? extends Annotation> mAnnotation;
    private Object mRepresentedObject;

    public Builder type(TypeName type) {
      mType = type;
      return this;
    }

    public Builder name(String name) {
      mName = name;
      return this;
    }

    public Builder annotation(Class<? extends Annotation> annotation) {
      mAnnotation = annotation;
      return this;
    }

    public Builder representedObject(Object representedObject) {
      mRepresentedObject = representedObject;
      return this;
    }

    public TestMethodParamModel build() {
      return new TestMethodParamModel(mType, mName, mAnnotation, mRepresentedObject);
    }
  }
}
