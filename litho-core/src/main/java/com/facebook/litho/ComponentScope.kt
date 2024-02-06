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

import android.view.View

/**
 * The implicit receiver for [KComponent.render] call. This class exposes the ability to use hooks,
 * like [useState], and convenience functions, like [dp].
 */
open class ComponentScope(
    override val context: ComponentContext,
    internal var resolveContext: ResolveContext? = null
) : ResourcesScope {
  // TODO: Extract into more generic container to track hooks when needed
  internal var useStateIndex = 0
  internal var useCachedIndex = 0
  internal var transitions: MutableList<Transition>? = null
  internal var useEffectEntries: MutableList<Attachable>? = null

  /**
   * A utility function to find the View with a given tag under the current Component's LithoView.
   * To set a view tag, use Style.viewTag. An appropriate time to call this is in your Component's
   * onVisible callback.
   *
   * <p>As with View.findViewWithTag in general, this must be called on the main thread.
   *
   * <p>Note that null may be returned if the associated View doesn't exist or isn't mounted: with
   * incremental mount turned on (which is the default), if the component is off-screen, it won't be
   * mounted.
   *
   * <p>Finally, note that you should never hold a reference to the view returned by this function
   * as Litho may unmount your Component and mount it to a different View.
   */
  fun <T : View> findViewWithTag(tag: Any): T? {
    return context.findViewWithTag(tag)
  }

  internal fun cleanUp() {
    resolveContext = null
  }
}
