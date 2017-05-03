/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.ResType;
import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

import com.facebook.litho.specmodels.internal.ImmutableList;

import com.squareup.javapoet.TypeName;

/**
 * Model that is a simple base representation of a
 * {@link com.facebook.litho.annotations.PropDefault}.
 */
@Immutable
public class PropDefaultModel {
  public final TypeName mType;
  public final String mName;
  public final ImmutableList<Modifier> mModifiers;
  public final Object mRepresentedObject;
  public final ResType mResType;

  public PropDefaultModel(
      TypeName type,
      String name,
      ImmutableList<Modifier> modifiers,
      Object representedObject,
      ResType resType) {
    mType = type;
    mName = name;
    mModifiers = modifiers;
    mRepresentedObject = representedObject;
    mResType = resType;
  }

  public boolean hasResType() {
    return mResType != ResType.NONE;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PropDefaultModel) {
      final PropDefaultModel p = (PropDefaultModel) o;
      return mType.equals(p.mType) && mName.equals(p.mName) && mModifiers.equals(p.mModifiers)
          && mResType.equals(p.mResType);
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = mType.hashCode();
    result = 17 * result + mName.hashCode();
    result = 31 * result + mModifiers.hashCode();
    result = 31 * result + mResType.hashCode();
    return result;
  }
}
