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

package com.facebook.samples.litho.kotlin.mountandvisibility

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.LithoView
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.surface.getLithoView
import com.facebook.litho.surface.setLithoContent
import com.facebook.litho.view.backgroundColor
import com.facebook.rendercore.dp
import com.facebook.rendercore.extensions.VisibilityChangeReason
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/**
 * Demo Activity showing manual triggering of incremental mount.
 *
 * This demonstrates how to manually call
 * `lithoView.notifyVisibleBoundsChanged(VisibilityChangeReason.MOUNT)` to trigger incremental mount
 * recalculation. This is useful when:
 * - A custom scroll container is used that Litho doesn't automatically detect
 * - The visible bounds change without a scroll event (e.g., animating views)
 * - You need to force a mount/unmount cycle
 *
 * The demo shows:
 * 1. A LithoView with items that log mount/unmount events
 * 2. Buttons to manually trigger incremental mount with different visible rects
 * 3. Visual feedback showing which items are mounted/unmounted
 */
@SuppressLint("BadSuperClassFragmentActivity.FbFragmentActivity", "EndpointWithoutSwitchOff")
class IncrementalMountManualTriggerActivity : FragmentActivity() {

  private lateinit var lithoView: LithoView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setLithoContent { ManualMountDemoComponent() }
    lithoView = getLithoView()

    val rootLayout =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setBackgroundColor(Color.WHITE)
        }

    val controlPanel =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(32, 32, 32, 32)
          setBackgroundColor("#F5F5F5".toColorInt())
        }

    val instructionText =
        android.widget.TextView(this).apply {
          text =
              "Manual Incremental Mount Demo\n\n" +
                  "Use buttons below to manually trigger notifyVisibleBoundsChanged() " +
                  "with different visible rects. Watch the logs to see MOUNT/UNMOUNT events."
          textSize = 14f
          setTextColor(Color.DKGRAY)
        }
    controlPanel.addView(instructionText)

    // start_example
    // Button: Full visible bounds
    val fullBoundsButton =
        Button(this).apply {
          text = "Mount All (Full Bounds)"
          setOnClickListener {
            Log.d(TAG, "=== Triggering notifyVisibleBoundsChanged() with full bounds ===")
            // Recalculate based on current visible bounds (manual trigger = VISIBILITY_HINT)
            lithoView.notifyVisibleBoundsChanged(VisibilityChangeReason.VISIBILITY_HINT)
          }
        }
    controlPanel.addView(fullBoundsButton)

    // Button: Top half only
    val topHalfButton =
        Button(this).apply {
          text = "Mount Top Half Only"
          setOnClickListener {
            // Specify a custom visible rect to mount only components in that area
            val rect = Rect(0, 0, lithoView.width, lithoView.height / 2)
            Log.d(TAG, "=== Triggering notifyVisibleBoundsChanged($rect) - Top Half ===")
            lithoView.notifyVisibleBoundsChanged(rect, true, VisibilityChangeReason.UNKNOWN)
          }
        }
    controlPanel.addView(topHalfButton)

    // Button: Bottom half only
    val bottomHalfButton =
        Button(this).apply {
          text = "Mount Bottom Half Only"
          setOnClickListener {
            val height = lithoView.height
            val rect = Rect(0, height / 2, lithoView.width, height)
            Log.d(TAG, "=== Triggering notifyVisibleBoundsChanged($rect) - Bottom Half ===")
            lithoView.notifyVisibleBoundsChanged(rect, true, VisibilityChangeReason.UNKNOWN)
          }
        }
    controlPanel.addView(bottomHalfButton)

    // Button: Nothing visible (unmount all)
    val unmountAllButton =
        Button(this).apply {
          text = "Unmount All (Empty Rect)"
          setOnClickListener {
            // Pass a rect outside visible area to unmount all components
            val rect = Rect(0, -1000, lithoView.width, -900)
            Log.d(TAG, "=== Triggering notifyVisibleBoundsChanged($rect) - Unmount All ===")
            lithoView.notifyVisibleBoundsChanged(rect, true, VisibilityChangeReason.UNKNOWN)
          }
        }
    controlPanel.addView(unmountAllButton)
    // end_example

    rootLayout.addView(
        controlPanel,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    val lithoContainer = FrameLayout(this).apply { setBackgroundColor("#E3F2FD".toColorInt()) }

    lithoContainer.addView(
        lithoView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )

    rootLayout.addView(
        lithoContainer,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ),
    )

    setContentView(rootLayout)
  }

  companion object {
    private const val TAG = "ManualIM"
  }
}

/** Component showing a list of items that log mount/unmount events. */
class ManualMountDemoComponent(
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    return Column(style = style + Style.flex(grow = 1f).padding(all = 16.dp)) {
      child(
          Text(
              text = "Items below will log MOUNT/UNMOUNT events",
              textSize = 14.sp,
              style = Style.padding(bottom = 16.dp),
          ),
      )

      for (i in 1..10) {
        child(ManualMountItemComponent(itemIndex = i))
      }
    }
  }
}

/** Individual item that logs when it's mounted/unmounted. */
class ManualMountItemComponent(
    private val itemIndex: Int,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val isEven = itemIndex % 2 == 0
    val bgColor = if (isEven) "#FFFFFF".toColorInt() else "#F5F5F5".toColorInt()

    return Row(
        alignItems = YogaAlign.CENTER,
        style =
            style +
                Style.flex(shrink = 0f)
                    .height(50.dp)
                    .margin(bottom = 8.dp)
                    .padding(horizontal = 16.dp)
                    .backgroundColor(bgColor),
    ) {
      child(
          ManualMountIndicator(
              tag = "ManualIM",
              itemId = "Item$itemIndex",
              style = Style.width(24.dp).height(24.dp).margin(end = 12.dp),
          ),
      )
      child(
          Text(
              text = "Item $itemIndex",
              textSize = 16.sp,
              style = Style.flex(shrink = 0f),
          ),
      )
    }
  }
}

/**
 * Primitive that logs mount/unmount events for manual incremental mount demo.
 *
 * Shows a gray box when mounted, logs when mount/unmount occurs.
 */
class ManualMountIndicator(
    private val tag: String,
    private val itemId: String,
    private val style: Style? = null,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = FixedSizeLayoutBehavior(24.px, 24.px),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> View(context) }) {
              bind(itemId) { content ->
                ViewCompat.setBackground(content, ColorDrawable(Color.GREEN))
                Log.d(tag, "MOUNTED: $itemId")

                onUnbind {
                  ViewCompat.setBackground(content, ColorDrawable(Color.RED))
                  Log.d(tag, "UNMOUNTED: $itemId")
                }
              }
            },
        style = style,
    )
  }
}
