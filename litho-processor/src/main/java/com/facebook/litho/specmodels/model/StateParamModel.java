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
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/** Model that is an abstract representation of a {@link com.facebook.litho.annotations.State}. */
@Immutable
public class StateParamModel implements MethodParamModel {
  private final MethodParamModel mParamModel;
  private final boolean mCanUpdateLazily;

  StateParamModel(MethodParamModel paramModel, boolean canUpdateLazily) {
    mParamModel = paramModel;
    mCanUpdateLazily = canUpdateLazily;
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

  public boolean canUpdateLazily() {
    return mCanUpdateLazily;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof StateParamModel) {
      final StateParamModel p = (StateParamModel) o;
      return mParamModel.equals(p.mParamModel) && mCanUpdateLazily == p.mCanUpdateLazily;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mParamModel.hashCode() + (mCanUpdateLazily ? 1 : 0);
  }
}
