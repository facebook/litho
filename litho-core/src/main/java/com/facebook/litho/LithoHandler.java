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

import android.os.Handler;
import android.os.Looper;
import com.facebook.infer.annotation.Nullsafe;

/**
 * The Litho handler is responsible for scheduling computations on a {@link ComponentTree}. The
 * default implementation uses a {@link android.os.Handler} with a {@link android.os.Looper}.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface LithoHandler {

  boolean isTracing();

  void post(Runnable runnable, String tag);

  void postAtFront(Runnable runnable, String tag);

  void remove(Runnable runnable);

  /** Default implementation of the LithoHandler which simply wraps an {@link Handler}. */
  class DefaultLithoHandler extends Handler implements LithoHandler {

    public DefaultLithoHandler(Looper looper) {
      super(looper);
    }

    @Override
    public boolean isTracing() {
      return false;
    }

    @Override
    public void post(Runnable runnable, String tag) {
      post(runnable);
    }

    @Override
    public void postAtFront(Runnable runnable, String tag) {
      postAtFrontOfQueue(runnable);
    }

    @Override
    public void remove(Runnable runnable) {
      removeCallbacks(runnable);
    }
  }
}
