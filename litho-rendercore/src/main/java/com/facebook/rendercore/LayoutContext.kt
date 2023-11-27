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

package com.facebook.rendercore

import android.content.Context
import com.facebook.rendercore.extensions.RenderCoreExtension

/**
 * A LayoutContext encapsulates all the data needed during a layout pass. It contains - The Android
 * context associated with this layout calculation. - The version of the layout calculation. - The
 * LayoutCache for this layout calculation. Access to the cache is only valid during the execution
 * of the Node's calculateLayout function.
 */
class LayoutContext<RenderContext>(
    val androidContext: Context,
    val renderContext: RenderContext?,
    val layoutVersion: Int,
    private var _layoutCache: LayoutCache?,
    val extensions: Array<RenderCoreExtension<*, *>>?
) {

  var layoutContextExtraData: LayoutContextExtraData<*>? = null
  private var previousLayoutData: Any? = null
  val layoutCache: LayoutCache by
      lazy(LazyThreadSafetyMode.NONE) {
        checkNotNull(_layoutCache) { "Trying to access the LayoutCache from outside a layout call" }
      }

  fun clearCache() {
    _layoutCache = null
  }

  fun setPreviousLayoutDataForCurrentNode(previousLayoutData: Any?) {
    this.previousLayoutData = previousLayoutData
  }

  fun consumePreviousLayoutDataForCurrentNode(): Any? {
    val data = previousLayoutData
    previousLayoutData = null
    return data
  }
}
