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

package com.facebook.litho;

import android.content.Context;

public class TrackingMountContentPool extends DefaultMountContentPool {

  private int mAcquireCount = 0;
  private int mReleaseCount = 0;

  public TrackingMountContentPool(String name, int maxSize, boolean sync) {
    super(name, maxSize, sync);
  }

  @Override
  public Object acquire(Context c, ComponentLifecycle lifecycle) {
    Object item = super.acquire(c, lifecycle);
    mAcquireCount++;
    return item;
  }

  @Override
  public void release(Object item) {
    super.release(item);
    mReleaseCount++;
  }

  public int getAcquireCount() {
    return mAcquireCount;
  }

  public int getReleaseCount() {
    return mReleaseCount;
  }
}
