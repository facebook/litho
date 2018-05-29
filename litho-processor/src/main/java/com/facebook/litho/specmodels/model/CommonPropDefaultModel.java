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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.TypeName;
import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

/**
 * Model that is a simple base representation of a {@link
 * com.facebook.litho.annotations.CommonPropDefault}.
 */
@Immutable
public class CommonPropDefaultModel {
  public final TypeName mType;
  public final ImmutableList<Modifier> mModifiers;
  public final Object mRepresentedObject;
  public final String mName;

  public CommonPropDefaultModel(
      TypeName type, String name, ImmutableList<Modifier> modifiers, Object representedObject) {
    mType = type;
    mName = name;
    mModifiers = modifiers;
    mRepresentedObject = representedObject;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof CommonPropDefaultModel) {
      final CommonPropDefaultModel p = (CommonPropDefaultModel) o;
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

  @Override
  public String toString() {
    return "CommonPropDefaultModel{"
        + "mType="
        + mType
        + ", mModifiers="
        + mModifiers
        + ", mRepresentedObject="
        + mRepresentedObject
        + ", mName='"
        + mName
        + '\''
        + '}';
  }
}
