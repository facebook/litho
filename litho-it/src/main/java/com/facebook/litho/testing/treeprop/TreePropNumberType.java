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

package com.facebook.litho.testing.treeprop;

public class TreePropNumberType {

  private final int mValue;

  public TreePropNumberType(int value) {
    mValue = value;
  }

  public Integer getValue() {
    return mValue;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    TreePropNumberType other = (TreePropNumberType) obj;
    return other.getValue() == mValue;
  }

  @Override
  public int hashCode() {
    return mValue;
  }

  @Override
  public String toString() {
    return "TreePropNumberType{" + "mValue=" + mValue + '}';
  }
}
