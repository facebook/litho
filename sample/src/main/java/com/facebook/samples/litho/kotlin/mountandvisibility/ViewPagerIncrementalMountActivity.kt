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
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
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
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/** Demo Activity showing Incremental Mount with legacy ViewPager. */
@SuppressLint("BadSuperClassFragmentActivity.FbFragmentActivity", "EndpointWithoutSwitchOff")
class ViewPagerIncrementalMountActivity : FragmentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val viewPager = ViewPager(this)
    viewPager.adapter = ViewPagerDemoAdapter()

    viewPager.addOnPageChangeListener(
        object : ViewPager.OnPageChangeListener {
          override fun onPageScrolled(
              position: Int,
              positionOffset: Float,
              positionOffsetPixels: Int,
          ) {
            if (positionOffset > 0) {
              Log.d(
                  TAG,
                  "onPageScrolled: position=$position, offset=$positionOffset - IncrementalMountHelper triggers notifyVisibleBoundsChanged()",
              )
            }
          }

          override fun onPageSelected(position: Int) {
            Log.d(TAG, "onPageSelected: page $position")
          }

          override fun onPageScrollStateChanged(state: Int) {
            val stateName =
                when (state) {
                  ViewPager.SCROLL_STATE_IDLE -> "IDLE"
                  ViewPager.SCROLL_STATE_DRAGGING -> "DRAGGING"
                  ViewPager.SCROLL_STATE_SETTLING -> "SETTLING"
                  else -> "UNKNOWN"
                }
            Log.d(TAG, "onPageScrollStateChanged: $stateName")
          }
        },
    )

    setContentView(viewPager)
  }

  companion object {
    private const val TAG = "ViewPagerIM"
  }
}

// start_example
/** Adapter for legacy ViewPager that creates pages with LithoViews. */
class ViewPagerDemoAdapter : PagerAdapter() {

  override fun getCount(): Int = 5

  override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj

  override fun instantiateItem(container: ViewGroup, position: Int): Any {
    // Legacy ViewPager: LithoView automatically supports incremental mount
    // No special configuration needed
    val lithoView =
        LithoView.create(container.context, ViewPagerPageComponent(pageIndex = position))
    container.addView(lithoView)
    return lithoView
  }

  override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
    container.removeView(obj as View)
  }
}

// end_example

/** Component for a single ViewPager page showing mount behavior. */
class ViewPagerPageComponent(
    private val pageIndex: Int,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val colors = listOf(
        "#E3F2FD".toColorInt(),
        "#E8F5E9".toColorInt(),
        "#FFF3E0".toColorInt(),
        "#FCE4EC".toColorInt(),
        "#F3E5F5".toColorInt(),
    )

    return Column(
        style = style + Style.flex(grow = 1f).backgroundColor(colors[pageIndex % colors.size]),
    ) {
      child(
          Column(style = Style.flex(shrink = 0f).height(120.dp).padding(all = 16.dp)) {
            child(
                Text(
                    text = "Page ${pageIndex + 1}",
                    textSize = 24.sp,
                    style = Style.flex(shrink = 0f),
                ),
            )
            child(
                Text(
                    text = "Swipe to see incremental mount in action",
                    textSize = 14.sp,
                    style = Style.flex(shrink = 0f).padding(top = 8.dp),
                ),
            )
            child(
                Text(
                    text = "Legacy ViewPager: Automatic IM support",
                    textSize = 12.sp,
                    style = Style.flex(shrink = 0f).padding(top = 4.dp),
                ),
            )
          },
      )

      child(
          LazyList(style = Style.flex(grow = 1f)) {
            for (i in 1..50) {
              child(
                  id = i,
                  component =
                      SimplePageItemComponent(
                          pageIndex = pageIndex,
                          itemIndex = i,
                      ),
              )
            }
          },
      )
    }
  }
}

/** Simple item component for ViewPager pages that logs mount/unmount events. */
class SimplePageItemComponent(
    private val pageIndex: Int,
    private val itemIndex: Int,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val isEven = itemIndex % 2 == 0
    val bgColor = if (isEven) "#FFFFFF".toColorInt() else "#FAFAFA".toColorInt()

    return Row(
        alignItems = YogaAlign.CENTER,
        style =
            style +
                Style.flex(shrink = 0f)
                    .height(60.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .backgroundColor(bgColor),
    ) {
      child(
          MountLoggerPrimitive(
              tag = "ViewPagerIM",
              itemId = "P${pageIndex + 1}-Item$itemIndex",
              style = Style.width(24.dp).height(24.dp).margin(end = 12.dp),
          ),
      )
      child(
          Text(
              text = "Page ${pageIndex + 1} - Item $itemIndex",
              textSize = 16.sp,
              style = Style.flex(shrink = 0f),
          ),
      )
    }
  }
}

/**
 * A Primitive component that logs mount/unmount events.
 *
 * This demonstrates incremental mount - when items scroll into view, they get MOUNTED (gray box
 * appears). When items scroll out of view, they get UNMOUNTED (logged).
 */
class MountLoggerPrimitive(
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
                ViewCompat.setBackground(content, ColorDrawable(Color.GRAY))
                Log.d(tag, "MOUNTED: $itemId")

                onUnbind {
                  ViewCompat.setBackground(content, ColorDrawable(Color.LTGRAY))
                  Log.d(tag, "UNMOUNTED: $itemId")
                }
              }
            },
        style = style,
    )
  }
}
