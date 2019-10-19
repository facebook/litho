/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.specmodels;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.TypeSpec;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.assertj.core.util.Lists;

/** A simple implementation of {@link MockMethodParamModel} for use in tests. */
@Immutable
public class MockMethodParamModel implements MethodParamModel {
  private final TypeSpec mTypeSpec;
  private final String mName;
  private final List<Annotation> mAnnotations;
  private final List<AnnotationSpec> mExternalAnnotations;
  private final Object mRepresentedObject;

  public MockMethodParamModel(
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

  // This won't return a full TypeSpec hierarchy because wasn't generated with a TypeMirror.
  // It's going to be a TypeSpec only wrapping the associated TypeName.
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

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private TypeName mType;
    private String mName;
    private List<Class<? extends Annotation>> mAnnotations = ImmutableList.of();
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

    public Builder annotations(List<Class<? extends Annotation>> annotations) {
      mAnnotations = annotations;
      return this;
    }

    public Builder annotations(Class<? extends Annotation>... annotations) {
      mAnnotations = Lists.newArrayList(annotations);
      return this;
    }

    public Builder externalAnnotations(AnnotationSpec... externalAnnotations) {
      mExternalAnnotations = Lists.newArrayList(externalAnnotations);
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
      final List<Annotation> annotations = new ArrayList<>(mAnnotations.size());
      for (final Class<? extends Annotation> annotation : mAnnotations) {
        annotations.add(
            new Annotation() {
              @Override
              public Class<? extends Annotation> annotationType() {
                return annotation;
              }
            });
      }
      return new MockMethodParamModel(
          new TypeSpec(mType), mName, annotations, mExternalAnnotations, mRepresentedObject);
    }
  }
}
