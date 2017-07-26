/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor.integration.resources;

public class TestTreeProp {

  final private long mValue;

  public TestTreeProp(long value) {
    mValue = value;
  }

  public long getValue() {
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
    TestTreeProp other = (TestTreeProp) obj;
    return other.getValue() == mValue;
  }

  @Override
  public int hashCode() {
    return (int) mValue;
  }
}
