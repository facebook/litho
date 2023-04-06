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
import com.facebook.rendercore.MountItem
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import kotlin.jvm.JvmField

/** This class hosts any extra mount data related to MountItem. */
class LithoMountData(content: Any?) {

  // Flags that track view-related behaviour of mounted view content.
  val defaultAttributeValuesFlags: Int = getViewAttributeFlags(content)

  @JvmField var isReleased = false

  @JvmField var releaseCause: String? = null

  companion object {
    private const val FLAG_VIEW_CLICKABLE = 1 shl 0
    private const val FLAG_VIEW_LONG_CLICKABLE = 1 shl 1
    private const val FLAG_VIEW_FOCUSABLE = 1 shl 2
    private const val FLAG_VIEW_ENABLED = 1 shl 3
    private const val FLAG_VIEW_SELECTED = 1 shl 4
    private const val FLAG_VIEW_LAYER_TYPE_0 = 1 shl 5
    private const val FLAG_VIEW_LAYER_TYPE_1 = 1 shl 6

    /** @return Whether the view associated with this MountItem is clickable. */
    @JvmStatic
    fun isViewClickable(flags: Int): Boolean = flags and FLAG_VIEW_CLICKABLE == FLAG_VIEW_CLICKABLE

    /** @return Whether the view associated with this MountItem is long clickable. */
    @JvmStatic
    fun isViewLongClickable(flags: Int): Boolean =
        flags and FLAG_VIEW_LONG_CLICKABLE == FLAG_VIEW_LONG_CLICKABLE

    /** @return Whether the view associated with this MountItem is setFocusable. */
    @JvmStatic
    fun isViewFocusable(flags: Int): Boolean = flags and FLAG_VIEW_FOCUSABLE == FLAG_VIEW_FOCUSABLE

    /** @return Whether the view associated with this MountItem is setEnabled. */
    @JvmStatic
    fun isViewEnabled(flags: Int): Boolean = flags and FLAG_VIEW_ENABLED == FLAG_VIEW_ENABLED

    /** @return Whether the view associated with this MountItem is setSelected. */
    @JvmStatic
    fun isViewSelected(flags: Int): Boolean = flags and FLAG_VIEW_SELECTED == FLAG_VIEW_SELECTED

    @JvmStatic
    @LayerType
    fun getOriginalLayerType(flags: Int): Int =
        if (flags and FLAG_VIEW_LAYER_TYPE_0 == 0) {
          LayerType.LAYER_TYPE_NOT_SET
        } else if (flags and FLAG_VIEW_LAYER_TYPE_1 == FLAG_VIEW_LAYER_TYPE_1) {
          LayerType.LAYER_TYPE_HARDWARE
        } else {
          LayerType.LAYER_TYPE_SOFTWARE
        }

    @JvmStatic
    fun getMountData(item: MountItem): LithoMountData {
      return item.mountData as? LithoMountData
          ?: throw RuntimeException("MountData should not be null when using Litho's MountState.")
    }

    @JvmStatic
    fun getViewAttributeFlags(content: Any?): Int {
      var flags = 0
      if (content is View) {
        if (content.isClickable) {
          flags = flags or FLAG_VIEW_CLICKABLE
        }
        if (content.isLongClickable) {
          flags = flags or FLAG_VIEW_LONG_CLICKABLE
        }
        if (content.isFocusable) {
          flags = flags or FLAG_VIEW_FOCUSABLE
        }
        if (content.isEnabled) {
          flags = flags or FLAG_VIEW_ENABLED
        }
        if (content.isSelected) {
          flags = flags or FLAG_VIEW_SELECTED
        }
        when (content.layerType) {
          View.LAYER_TYPE_NONE -> {}
          View.LAYER_TYPE_SOFTWARE -> flags = flags or FLAG_VIEW_LAYER_TYPE_0
          View.LAYER_TYPE_HARDWARE -> flags = flags or FLAG_VIEW_LAYER_TYPE_1
          else -> throw IllegalArgumentException("Unhandled layer type encountered.")
        }
      }
      return flags
    }
  }
}
