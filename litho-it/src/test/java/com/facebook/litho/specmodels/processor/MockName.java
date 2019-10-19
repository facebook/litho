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
