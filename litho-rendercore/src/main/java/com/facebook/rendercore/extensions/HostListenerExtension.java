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

package com.facebook.rendercore.extensions;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Host;

/** A type of mount extension which can subscribe to a callbacks from a {@link Host} view. */
public interface HostListenerExtension<T> {

  /**
   * Called for setting up input on the extension before mounting.
   *
   * @param input The new input the extension should use.
   */
  void beforeMount(T input, @Nullable Rect localVisibleRect);

  /** Called immediately after mounting. */
  void afterMount();

  /** Called when the visible bounds of the Host change. */
  void onVisibleBoundsChanged(@Nullable Rect localVisibleRect);

  /** Called after all the Host's children have been unmounted. */
  void onUnmount();

  /** Called after all the Host's children have been unbound. */
  void onUnbind();
}
