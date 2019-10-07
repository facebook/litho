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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.Prop} that
 * has type Diff.
 */
@Immutable
public class DiffPropModel implements MethodParamModel {
  private final PropModel mUnderlyingPropModel;

  DiffPropModel(PropModel underlyingPropModel) {
    mUnderlyingPropModel = underlyingPropModel;
  }

  @Override
  public TypeSpec getTypeSpec() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeName getTypeName() {
    return ParameterizedTypeName.get(ClassNames.DIFF, mUnderlyingPropModel.getTypeName().box());
  }

  @Override
  public String getName() {
    return mUnderlyingPropModel.getName();
  }

  @Override
  public List<Annotation> getAnnotations() {
    return mUnderlyingPropModel.getAnnotations();
  }

  @Override
  public List<AnnotationSpec> getExternalAnnotations() {
    return mUnderlyingPropModel.getExternalAnnotations();
  }

  @Override
  public Object getRepresentedObject() {
    return mUnderlyingPropModel.getRepresentedObject();
  }

  public PropModel getUnderlyingPropModel() {
    return mUnderlyingPropModel;
  }

  public boolean isOptional() {
    return mUnderlyingPropModel.isOptional();
  }

  public ResType getResType() {
    return mUnderlyingPropModel.getResType();
  }

  public boolean hasVarArgs() {
    return !mUnderlyingPropModel.hasVarArgs();
  }

  public String getVarArgsSingleName() {
    return mUnderlyingPropModel.getVarArgsSingleName();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DiffPropModel) {
      final DiffPropModel p = (DiffPropModel) o;
      return mUnderlyingPropModel.equals(p.mUnderlyingPropModel);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mUnderlyingPropModel.hashCode();
  }

  /**
   * Compare a given {@link PropModel} to the underlying propmodel of this instance. If a cached
   * name is provided, it will override the check with the underlying model.
   */
  public boolean isSameUnderlyingPropModel(PropModel propModel, @Nullable String cachedName) {
    return (propModel.getName().equals(getName()) || propModel.getName().equals(cachedName))
        && propModel.getTypeName().box().equals(mUnderlyingPropModel.getTypeName().box())
        && propModel.isOptional() == isOptional()
        && propModel.getResType() == getResType()
        && propModel.getVarArgsSingleName().equals(getVarArgsSingleName());
  }
}
