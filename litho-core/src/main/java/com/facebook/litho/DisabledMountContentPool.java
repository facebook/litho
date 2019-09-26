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

import android.content.Context;

/**
 * A MountContentPool that has no size and doesn't recycle objects. Return from
 * OnCreateMountContentPool to disable recycling.
 */
public class DisabledMountContentPool implements MountContentPool {

  @Override
  public Object acquire(Context c, ComponentLifecycle lifecycle) {
    return lifecycle.createMountContent(c);
  }

  @Override
  public void release(Object item) {}

  @Override
  public void maybePreallocateContent(Context c, ComponentLifecycle lifecycle) {}

  @Override
  public String getName() {
    return "DisabledMountContentPool";
  }

  @Override
  public int getMaxSize() {
    return 0;
  }

  @Override
  public int getCurrentSize() {
    return 0;
  }
}
