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

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
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
    private const val FLAG_VIEW_NOT_FOCUSABLE = 1 shl 3
    private const val FLAG_VIEW_FOCUSABLE_AUTO = 1 shl 4
    private const val FLAG_VIEW_ENABLED = 1 shl 5
    private const val FLAG_VIEW_SELECTED = 1 shl 6
    private const val FLAG_VIEW_LAYER_TYPE_0 = 1 shl 7
    private const val FLAG_VIEW_LAYER_TYPE_1 = 1 shl 8
    private const val FLAG_VIEW_KEYBOARD_NAVIGATION_CLUSTER = 1 shl 9
    private const val FLAG_VIEW_VISIBILITY_1 = 1 shl 10
    private const val FLAG_VIEW_VISIBILITY_2 = 1 shl 11

    /** @return Whether the view associated with this MountItem is clickable. */
    @JvmStatic
    fun isViewClickable(flags: Int): Boolean = flags and FLAG_VIEW_CLICKABLE == FLAG_VIEW_CLICKABLE

    /** @return Whether the view associated with this MountItem is long clickable. */
    @JvmStatic
    fun isViewLongClickable(flags: Int): Boolean =
        flags and FLAG_VIEW_LONG_CLICKABLE == FLAG_VIEW_LONG_CLICKABLE

    /** @return Whether the view associated with this MountItem is setFocusable. */
    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun getViewFocusable(flags: Int): Int =
        if (flags and FLAG_VIEW_FOCUSABLE == FLAG_VIEW_FOCUSABLE) {
          View.FOCUSABLE
        } else if (flags and FLAG_VIEW_NOT_FOCUSABLE == FLAG_VIEW_NOT_FOCUSABLE) {
          View.NOT_FOCUSABLE
        } else {
          // This is the default value since API 26.
          View.FOCUSABLE_AUTO
        }

    /** @return Whether the view associated with this MountItem is setFocusable. */
    @JvmStatic
    fun isViewFocusable(flags: Int): Boolean = flags and FLAG_VIEW_FOCUSABLE == FLAG_VIEW_FOCUSABLE

    /** @return Whether the view associated with this MountItem is setEnabled. */
    @JvmStatic
    fun isViewEnabled(flags: Int): Boolean = flags and FLAG_VIEW_ENABLED == FLAG_VIEW_ENABLED

    /** @return Whether the view associated with this MountItem is setSelected. */
    @JvmStatic
    fun isViewSelected(flags: Int): Boolean = flags and FLAG_VIEW_SELECTED == FLAG_VIEW_SELECTED

    /** @return Whether the view associated with this MountItem is setSelected. */
    @JvmStatic
    fun isViewKeyboardNavigationCluster(flags: Int): Boolean =
        flags and FLAG_VIEW_KEYBOARD_NAVIGATION_CLUSTER == FLAG_VIEW_KEYBOARD_NAVIGATION_CLUSTER

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

    fun getOriginalVisibility(flags: Int): Int =
        if (flags and (FLAG_VIEW_VISIBILITY_1 or FLAG_VIEW_VISIBILITY_2) == 0) {
          View.VISIBLE
        } else if (flags and FLAG_VIEW_VISIBILITY_1 == FLAG_VIEW_VISIBILITY_1) {
          View.INVISIBLE
        } else {
          View.GONE
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          when (content.focusable) {
            View.FOCUSABLE -> flags = flags or FLAG_VIEW_FOCUSABLE
            View.NOT_FOCUSABLE -> flags = flags or FLAG_VIEW_NOT_FOCUSABLE
            View.FOCUSABLE_AUTO -> flags = flags or FLAG_VIEW_FOCUSABLE_AUTO
          }
        } else {
          if (content.isFocusable) {
            flags = flags or FLAG_VIEW_FOCUSABLE
          }
        }
        if (content.isEnabled) {
          flags = flags or FLAG_VIEW_ENABLED
        }
        if (content.isSelected) {
          flags = flags or FLAG_VIEW_SELECTED
        }
        if (ViewCompat.isKeyboardNavigationCluster(content)) {
          flags = flags or FLAG_VIEW_KEYBOARD_NAVIGATION_CLUSTER
        }
        when (content.visibility) {
          View.VISIBLE -> {} // 00
          View.INVISIBLE -> flags = flags or FLAG_VIEW_VISIBILITY_1 // 01
          View.GONE -> flags = flags or FLAG_VIEW_VISIBILITY_2 // 10
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
