// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

import com.facebook.common.internal.ImmutableList;

import com.squareup.javapoet.TypeName;

/**
 * Model that is a simple base representation of a
 * {@link com.facebook.components.annotations.PropDefault}.
 */
@Immutable
public class PropDefaultModel {
  public final TypeName mType;
  public final String mName;
  public final ImmutableList<Modifier> mModifiers;

  public PropDefaultModel(TypeName type, String name, ImmutableList<Modifier> modifiers) {
    mType = type;
    mName = name;
    mModifiers = modifiers;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PropDefaultModel) {
      final PropDefaultModel p = (PropDefaultModel) o;
      return mType.equals(p.mType) && mName.equals(p.mName) && mModifiers.equals(p.mModifiers);
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = mType.hashCode();
    result = 17 * result + mName.hashCode();
    result = 31 * result + mModifiers.hashCode();
    return result;
  }
}
