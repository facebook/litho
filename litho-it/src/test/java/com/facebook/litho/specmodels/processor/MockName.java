/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.specmodels.processor;

import javax.lang.model.element.Name;

class MockName implements Name {

  private final CharSequence mName;

  public MockName(final CharSequence name) {
    mName = name;
  }

  @Override
  public boolean contentEquals(CharSequence cs) {
    return mName.equals(cs);
  }

  @Override
  public int length() {
    return mName.length();
  }

  @Override
  public char charAt(int index) {
    return mName.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return mName.subSequence(start, end);
  }

  @Override
  public String toString() {
    return mName.toString();
  }
}
