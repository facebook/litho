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

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.Prop}.
 */
@Immutable
public class PropModel implements MethodParamModel {
  private final MethodParamModel mParamModel;
  private final boolean mIsOptional;
  private final ResType mResType;
  private final String mVarArgSingleArgName;

  public PropModel(
      MethodParamModel paramModel, boolean isOptional, ResType resType, String varArg) {
    mParamModel = paramModel;
    mIsOptional = isOptional;
    mResType = resType;
    mVarArgSingleArgName = varArg;
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

  public boolean isOptional() {
    return mIsOptional;
  }

  public ResType getResType() {
    return mResType;
  }

  public boolean hasVarArgs() {
    return !mVarArgSingleArgName.isEmpty();
  }

  public String getVarArgsSingleName() {
    return mVarArgSingleArgName;
  }

  /**
   * @return true if this prop has a default specified in the given set of defaults, false
   * otherwise.
   */
  public boolean hasDefault(ImmutableList<PropDefaultModel> propDefaults) {
    for (PropDefaultModel propDefault : propDefaults) {
      if (propDefault.mType.equals(mParamModel.getTypeName())
          && propDefault.mName.equals(mParamModel.getName())) {
        return true;
      }
    }

    return false;
  }

  /** @return a new {@link PropModel} instance with the given name overridden. */
  public PropModel withName(String name) {
    return new PropModel(mParamModel, mIsOptional, mResType, mVarArgSingleArgName) {
      @Override
      public String getName() {
        return name;
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PropModel) {
      final PropModel p = (PropModel) o;
      return mParamModel.equals(p.mParamModel)
          && mIsOptional == p.mIsOptional
          && mResType == p.mResType
          && mVarArgSingleArgName.equals(p.getVarArgsSingleName());
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = mParamModel.hashCode();
    result = 17 * result + (mIsOptional ? 1 : 0);
    result = mResType == null ? result : 31 * result + mResType.hashCode();
    result = 43 * result + mVarArgSingleArgName.hashCode();
    return result;
  }
}
