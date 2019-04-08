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

/**
 * The Litho handler is responsible for scheduling computations on a {@link ComponentTree}. The
 * default implementation uses a {@link android.os.Handler} with a {@link android.os.Looper}.
 */
public interface LithoHandler {
  boolean post(Runnable runnable);
  void removeCallbacks(Runnable runnable);
  void removeCallbacksAndMessages(Object token);
}
