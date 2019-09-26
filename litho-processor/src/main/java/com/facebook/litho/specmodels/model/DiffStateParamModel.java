/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.model;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.State} that
 * has type Diff.
 */
@Immutable
public class DiffStateParamModel implements MethodParamModel {
  private final StateParamModel mUnderlyingStateParamModel;

  DiffStateParamModel(StateParamModel stateParamModel) {
    mUnderlyingStateParamModel = stateParamModel;
  }

  @Override
  public TypeSpec getTypeSpec() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeName getTypeName() {
    return ParameterizedTypeName.get(
        ClassNames.DIFF, mUnderlyingStateParamModel.getTypeName().box());
  }

  @Override
  public String getName() {
    return mUnderlyingStateParamModel.getName();
  }

  @Override
  public List<Annotation> getAnnotations() {
    return mUnderlyingStateParamModel.getAnnotations();
  }

  @Override
  public List<AnnotationSpec> getExternalAnnotations() {
    return mUnderlyingStateParamModel.getExternalAnnotations();
  }

  @Override
  public Object getRepresentedObject() {
    return mUnderlyingStateParamModel.getRepresentedObject();
  }

  public StateParamModel getUnderlyingStateParamModel() {
    return mUnderlyingStateParamModel;
  }

  public boolean canUpdateLazily() {
    return mUnderlyingStateParamModel.canUpdateLazily();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DiffStateParamModel) {
      final DiffStateParamModel p = (DiffStateParamModel) o;
      return mUnderlyingStateParamModel.equals(p.mUnderlyingStateParamModel);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mUnderlyingStateParamModel.hashCode();
  }

  public boolean isSameUnderlyingStateValueModel(StateParamModel stateParamModel) {
    return stateParamModel.getName().equals(getName())
        && stateParamModel
            .getTypeName()
            .box()
            .equals(mUnderlyingStateParamModel.getTypeName().box())
        && stateParamModel.canUpdateLazily() == canUpdateLazily();
  }
}
