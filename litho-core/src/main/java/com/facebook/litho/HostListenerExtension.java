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

import android.graphics.Rect;

/** Mount extension which can subscribe to a Host to receive event notifications. */
public interface HostListenerExtension<T> {

  /**
   * Called for setting up input on the extension before mounting.
   *
   * @param input
   */
  void beforeMount(T input, Rect localVisibleRect);

  /** Called immediately after mounting. */
  void afterMount();

  /** Called when the visible bounds of the Host change. */
  void onVisibleBoundsChanged(Rect localVisibleRect);

  /** Called after all the Host's children have been unmounted. */
  void onUnmount();

  /** Called after all the Host's children have been unbound. */
  void onUnbind();
}
