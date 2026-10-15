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

package com.facebook.samples.litho.kotlin.documentation

import android.graphics.Color
import android.util.Log
import androidx.core.graphics.toColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.visibility.onInvisible
import com.facebook.litho.visibility.onVisible
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.rendercore.visibility.VisibilityChangeReason

/**
 * Example component demonstrating the use of VisibilityChangeReason.
 *
 * The VisibilityChangeReason field on VisibilityChangedEvent allows you to determine WHY a
 * visibility change occurred, not just WHAT changed. This enables more intelligent handling of
 * visibility events.
 *
 * For example:
 * - Pause video on LIFECYCLE, but keep playing during SCROLL if still partially visible
 * - Log different analytics events for scroll vs lifecycle visibility changes
 * - Avoid expensive operations during MOUNT if you know a SCROLL will follow
 *
 * To test different reasons:
 * - SCROLL: Scroll up and down in this view
 * - MOUNT: Initial display of components
 * - LIFECYCLE: Press home button and return to the app
 */
// start_example
class VisibilityChangeReasonExampleComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    return LazyList {
      child(
          Text(
              text = "Visibility Change Reason Demo",
              textSize = 20.sp,
              textColor = Color.BLACK,
              style = Style.padding(start = 16.dp, end = 16.dp, top = 16.dp),
          ),
      )
      child(
          Text(
              text = "Check logcat for visibility change events with tag 'VisibilityReason'.",
              textSize = 14.sp,
              textColor = Color.GRAY,
              style = Style.padding(start = 16.dp, end = 16.dp, top = 8.dp),
          ),
      )
      child(
          Text(
              text =
                  "• SCROLL: Scroll this view\n• MOUNT: Initial display\n• LIFECYCLE: Press home and return",
              textSize = 14.sp,
              textColor = Color.DKGRAY,
              style = Style.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
          ),
      )

      child(TrackedVisibilityComponent(label = "Top Component"))

      // Spacer items to enable scrolling and push Bottom Component off screen
      children(items = (1..20), id = { it }) { index ->
        Text(
            text = "Item $index",
            textSize = 16.sp,
            textColor = Color.DKGRAY,
            style = Style.padding(all = 16.dp),
        )
      }

      child(TrackedVisibilityComponent(label = "Bottom Component"))
    }
  }
}

/** A component that tracks and reports visibility change reasons via logcat. */
private class TrackedVisibilityComponent(
    private val label: String,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    return Column(
        style =
            Style.backgroundColor("#E3F2FD".toColorInt())
                .padding(16.dp)
                .onVisible { event ->
                  val reason = event.reason

                  Log.d(TAG, "[$label] VISIBLE - reason=${reason.name}")

                  when (reason) {
                    VisibilityChangeReason.SCROLL -> {
                      Log.d(TAG, "[$label] Component scrolled into view")
                    }
                    VisibilityChangeReason.MOUNT -> {
                      Log.d(TAG, "[$label] Component became visible after mount")
                    }
                    VisibilityChangeReason.LIFECYCLE -> {
                      Log.d(TAG, "[$label] App resumed - can restart playback")
                    }
                    VisibilityChangeReason.VISIBILITY_HINT -> {
                      Log.d(TAG, "[$label] Visibility hint changed")
                    }
                    VisibilityChangeReason.ANIMATION -> {
                      Log.d(TAG, "[$label] Animation completed")
                    }
                    VisibilityChangeReason.UNKNOWN -> {
                      Log.d(TAG, "[$label] Unknown visibility reason")
                    }
                  }
                }
                .onInvisible { event ->
                  val reason = event.reason

                  Log.d(TAG, "[$label] INVISIBLE - reason=${reason.name}")

                  when (reason) {
                    VisibilityChangeReason.SCROLL -> {
                      Log.d(TAG, "[$label] Component scrolled out of view")
                    }
                    VisibilityChangeReason.LIFECYCLE -> {
                      Log.d(TAG, "[$label] App paused - should pause playback")
                    }
                    else -> {
                      Log.d(TAG, "[$label] Component became invisible: ${reason.name}")
                    }
                  }
                },
    ) {
      child(
          Text(
              text = label,
              textSize = 18.sp,
              textColor = Color.BLACK,
          ),
      )
      child(
          Text(
              text = "Watch logcat for visibility events",
              textSize = 14.sp,
              textColor = Color.GRAY,
              style = Style.padding(top = 4.dp),
          ),
      )
    }
  }

  companion object {
    private const val TAG = "VisibilityReason"
  }
}
// end_example
