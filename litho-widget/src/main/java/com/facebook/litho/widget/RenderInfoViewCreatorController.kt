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

package com.facebook.litho.widget

import android.util.SparseArray
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.facebook.litho.viewcompat.ViewCreator

/**
 * Helper class to keep track of the different view types that we're rendering using ViewRenderInfo.
 */
class RenderInfoViewCreatorController(val componentViewType: Int) {

  @JvmField
  @VisibleForTesting
  val viewTypeToViewCreator: SparseArray<ViewCreator<*>> = SparseArray()

  @JvmField
  @VisibleForTesting
  val viewCreatorToViewType: MutableMap<ViewCreator<*>, Int> = HashMap()

  private val customViewTypeEnabled: Boolean = (componentViewType != DEFAULT_COMPONENT_VIEW_TYPE)
  private var viewTypeCounter: Int = componentViewType + 1

  @UiThread
  fun maybeTrackViewCreator(renderInfo: RenderInfo) {
    if (!renderInfo.rendersView()) {
      return
    }

    ensureCustomViewTypeValidity(renderInfo)

    val viewCreator = renderInfo.viewCreator
    val viewType: Int
    if (viewCreatorToViewType.containsKey(viewCreator)) {
      viewType = requireNotNull(viewCreatorToViewType[viewCreator])
    } else {
      viewType = if (renderInfo.hasCustomViewType()) renderInfo.viewType else viewTypeCounter++
      viewTypeToViewCreator.put(viewType, viewCreator)
      viewCreatorToViewType[viewCreator] = viewType
    }

    if (!renderInfo.hasCustomViewType()) {
      renderInfo.viewType = viewType
    }
  }

  private fun ensureCustomViewTypeValidity(renderInfo: RenderInfo) {
    if (customViewTypeEnabled && !renderInfo.hasCustomViewType()) {
      throw IllegalStateException(
          "If you enable custom viewTypes, you must provide a customViewType in ViewRenderInfo.")
    } else if (!customViewTypeEnabled && renderInfo.hasCustomViewType()) {
      throw IllegalStateException(
          "You must enable custom viewTypes to provide customViewType in ViewRenderInfo.")
    } else if (customViewTypeEnabled && componentViewType == renderInfo.viewType) {
      throw IllegalStateException("CustomViewType cannot be the same as ComponentViewType.")
    }
  }

  fun getViewCreator(viewType: Int): ViewCreator<*>? {
    return viewTypeToViewCreator[viewType]
  }

  companion object {
    const val DEFAULT_COMPONENT_VIEW_TYPE: Int = 0
  }
}
