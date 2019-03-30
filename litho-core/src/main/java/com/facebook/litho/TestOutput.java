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

package com.facebook.litho;

import android.graphics.Rect;

/**
 * Stores information about a {@link Component} which is only available when tests are run.
 * TestOutputs are calculated in {@link LayoutState} and transformed into {@link TestItem}s in
 * {@link MountState}.
 */
class TestOutput {
  private String mTestKey;
  private long mHostMarker = -1;
  private long mLayoutOutputId = -1;
  private final Rect mBounds = new Rect();

  String getTestKey() {
    return mTestKey;
  }

  void setTestKey(String testKey) {
    mTestKey = testKey;
  }

  Rect getBounds() {
    return mBounds;
  }

  void setBounds(Rect bounds) {
    mBounds.set(bounds);
  }

  void setBounds(int left, int top, int right, int bottom) {
    mBounds.set(left, top, right, bottom);
  }

  void setHostMarker(long hostMarker) {
    mHostMarker = hostMarker;
  }

  long getHostMarker() {
    return mHostMarker;
  }

  long getLayoutOutputId() {
    return mLayoutOutputId;
  }

  void setLayoutOutputId(long layoutOutputId) {
    mLayoutOutputId = layoutOutputId;
  }
}
