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

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.ResType;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.InjectProp}.
 */
@Immutable
public class InjectPropModel implements MethodParamModel {
  private final MethodParamModel mParamModel;
  private final boolean mIsLazy;

  public InjectPropModel(MethodParamModel paramModel, boolean isLazy) {
    mParamModel = paramModel;
    mIsLazy = isLazy;
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InjectPropModel that = (InjectPropModel) o;
    return mIsLazy == that.mIsLazy && Objects.equals(mParamModel, that.mParamModel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mParamModel, mIsLazy);
  }

  /** Convert to a regular prop model. */
  public PropModel toPropModel() {
    final String localName = getName();
    return new PropModel(mParamModel, false, false, false, false, ResType.NONE, "") {
      @Override
      public String getName() {
        return localName;
      }
    };
  }

  public boolean isLazy() {
    return mIsLazy;
  }

  /** @return a new {@link PropModel} instance with the given name overridden. */
  public InjectPropModel withName(String name) {
    return new InjectPropModel(mParamModel, mIsLazy) {
      @Override
      public String getName() {
        return name;
      }
    };
  }
}
