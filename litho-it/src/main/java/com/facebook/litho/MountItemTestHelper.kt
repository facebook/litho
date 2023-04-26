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
import com.facebook.rendercore.MountItem

object MountItemTestHelper {
  @JvmStatic
  fun create(
      component: Component,
      host: ComponentHost?,
      content: Any,
      info: NodeInfo?,
      bounds: Rect?,
      flags: Int,
      importantForAccessibility: Int
  ): MountItem {
    val unit: LithoRenderUnit =
        MountSpecLithoRenderUnit.create(
            0,
            component,
            null,
            info,
            bounds,
            flags,
            importantForAccessibility,
            LithoRenderUnit.STATE_UNKNOWN)
    val width = bounds?.width() ?: 0
    val height = bounds?.height() ?: 0
    val node =
        create(
            unit,
            bounds ?: Rect(),
            LithoLayoutData(
                width,
                height,
                0,
                0,
                null,
                null,
            ),
            null,
        )
    return MountItem(node, host, content).apply { mountData = LithoMountData(content) }
  }
}
