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
import com.facebook.litho.MountSpecLithoRenderUnit.UpdateState
import com.facebook.rendercore.RenderTreeNode

/**
 * This object will host the data associated with the component which is generated during the
 * measure pass, for example: the [InterStagePropsContainer], and the [UpdateState]. It will be
 * created in [LayoutResult#calculateLayout(LayoutContext, int, int)]. This object will be returned
 * by [LayoutResult#getLayoutData()], then written to the layout data in [RenderTreeNode] during
 * reduce.
 */
class LithoLayoutData(
    @field:JvmField val width: Int,
    @field:JvmField val height: Int,
    @field:JvmField val currentLayoutStateId: Int,
    @field:JvmField val previousLayoutStateId: Int,
    @field:JvmField val expandedTouchBounds: Rect?,
    @field:JvmField val layoutData: Any?
) {
  companion object {
    /**
     * Helper method to throw exception if a provided layout-data is null or not a LithoLayoutData
     * instance. Will return a casted, non-null instance of LithoLayoutData otherwise.
     */
    @JvmStatic
    fun verifyAndGetLithoLayoutData(layoutData: Any?): LithoLayoutData {
      if (layoutData == null) {
        throw RuntimeException("layout data must not be null.")
      }
      if (layoutData !is LithoLayoutData) {
        throw RuntimeException(
            "RenderTreeNode layout data for Litho should be LithoLayoutData but was <cls>${layoutData.javaClass.name}</cls>")
      }
      return layoutData
    }

    @JvmStatic
    fun getInterStageProps(data: Any?): InterStagePropsContainer? {
      val layoutData = verifyAndGetLithoLayoutData(data)
      if (layoutData.layoutData == null) {
        return null
      }
      if (layoutData.layoutData !is InterStagePropsContainer) {
        throw RuntimeException(
            "Layout data was not InterStagePropsContainer but was <cls>${layoutData.layoutData.javaClass.name}</cls>")
      }
      return layoutData.layoutData
    }

    @JvmStatic
    fun getExpandedTouchBounds(data: Any?): Rect? {
      return verifyAndGetLithoLayoutData(data).expandedTouchBounds
    }
  }
}
