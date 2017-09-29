/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.treeprop;

public class TreePropStringType {

  final private String mValue;

  public TreePropStringType(String value) {
    mValue = value;
  }

  public String getValue() {
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
    TreePropStringType other = (TreePropStringType) obj;
    return other.getValue().equals(mValue);
  }

  @Override
  public int hashCode() {
    return mValue.hashCode();
  }
}
