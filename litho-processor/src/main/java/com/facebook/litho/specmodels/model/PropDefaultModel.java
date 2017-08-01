/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.TypeName;
import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

/**
 * Model that is a simple base representation of a
 * {@link com.facebook.litho.annotations.PropDefault}.
 */
@Immutable
public class PropDefaultModel {
  public final TypeName mType;
  public final ImmutableList<Modifier> mModifiers;
  public final Object mRepresentedObject;
  public final String mName;
  private ResType mResType;
  private int mResId;

  public PropDefaultModel(
      TypeName type,
      String name,
      ImmutableList<Modifier> modifiers,
      Object representedObject) {
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
          && mResType.equals(p.mResType)
          && mResId == p.mResId;
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = mType.hashCode();
    result = 17 * result + mName.hashCode();
    result = 31 * result + mModifiers.hashCode();
    result = 43 * result + mResType.hashCode();
    result = 47 * result + mResId;
    return result;
  }
}
