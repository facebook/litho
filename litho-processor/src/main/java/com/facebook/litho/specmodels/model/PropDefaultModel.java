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
import com.squareup.javapoet.TypeName;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

/**
 * Model that is a simple base representation of a {@link
 * com.facebook.litho.annotations.PropDefault}.
 */
@Immutable
public class PropDefaultModel {
  public final TypeName mType;
  public final ImmutableList<Modifier> mModifiers;
  public final Object mRepresentedObject;
  public final String mName;
  @Nullable private ResType mResType;
  private int mResId;

  public PropDefaultModel(
      TypeName type, String name, ImmutableList<Modifier> modifiers, Object representedObject) {
    mType = type;
    mName = name;
    mModifiers = modifiers;
    mRepresentedObject = representedObject;
  }

  public PropDefaultModel(
      TypeName type,
      String name,
      ImmutableList<Modifier> modifiers,
      Object representedObject,
      ResType resType,
      int resId) {
    mType = type;
    mName = name;
    mModifiers = modifiers;
    mRepresentedObject = representedObject;
    mResType = resType;
    mResId = resId;
  }

  public String getName() {
    return mName;
  }

  @Nullable
  public ResType getResType() {
    return mResType;
  }

  public int getResId() {
    return mResId;
  }

  public boolean isResResolvable() {
    return hasResType() && hasResId();
  }

  private boolean hasResType() {
    return mResType != ResType.NONE;
  }

  private boolean hasResId() {
    return mResId != 0;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PropDefaultModel) {
      final PropDefaultModel p = (PropDefaultModel) o;
      return mType.equals(p.mType)
          && mName.equals(p.mName)
          && mModifiers.equals(p.mModifiers)
          && mResType == p.mResType
          && mResId == p.mResId;
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = mType.hashCode();
    result = 17 * result + mName.hashCode();
    result = 31 * result + mModifiers.hashCode();
    result = mResType == null ? result : 43 * result + mResType.hashCode();
    result = 47 * result + mResId;
    return result;
  }

  @Override
  public String toString() {
    return "PropDefaultModel{"
        + "mType="
        + mType
        + ", mModifiers="
        + mModifiers
        + ", mRepresentedObject="
        + mRepresentedObject
        + ", mName='"
        + mName
        + '\''
        + ", mResType="
        + mResType
        + ", mResId="
        + mResId
        + '}';
  }
}
