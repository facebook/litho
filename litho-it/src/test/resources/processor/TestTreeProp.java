// Copyright 2004-present Facebook. All Rights Reserved.

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
