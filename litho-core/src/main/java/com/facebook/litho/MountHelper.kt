/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho

import android.graphics.Rect

object MountHelper {

  /**
   * Can be used in the context of a ViewPager which binds views before ahead of displaying them and
   * requesting layout, if we want mount to happen ahead of time too.
   */
  @JvmStatic
  fun requestMount(
      componentTree: ComponentTree,
      visibleRect: Rect?,
      processVisibilityOutputs: Boolean
  ) {
    val lithoView = componentTree.lithoView
    lithoView?.mountComponent(visibleRect, processVisibilityOutputs)
  }

  private val emptyRect = Rect()

  @JvmStatic
  fun requestMount(lithoView: LithoView, processVisibilityOutputs: Boolean) {
    lithoView.mountComponent(emptyRect, processVisibilityOutputs)
  }
}
