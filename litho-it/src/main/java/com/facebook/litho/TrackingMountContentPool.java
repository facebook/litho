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

import android.annotation.SuppressLint;
import android.content.Context;
import java.util.HashMap;
import java.util.Map;

public final class TrackingMountContentPool extends RecyclePool implements MountContentPool {
  private static final Map<Class, Integer> sCounter = new HashMap<>();

  @SuppressLint("NewApi")
  public TrackingMountContentPool(Class<? extends Component> clazz) {
    super(clazz.getSimpleName(), 3, true);
    sCounter.put(clazz, sCounter.getOrDefault(clazz, 0) + 1);
  }

  @SuppressLint("NewApi")
  public static int getCounter(Class<? extends Component> clazz) {
    return sCounter.getOrDefault(clazz, 0);
  }

  public static void clearCounter() {
    sCounter.clear();
  }

  @Override
  public Object acquire(Context c, ComponentLifecycle lifecycle) {
    return lifecycle.onCreateMountContent(c);
  }

  @Override
  public void maybePreallocateContent(Context c, ComponentLifecycle lifecycle) {}
}
