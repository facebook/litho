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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoPrimitive
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
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/** Demo Activity showing Incremental Mount with ViewPager2. */
@SuppressLint("BadSuperClassFragmentActivity.FbFragmentActivity", "EndpointWithoutSwitchOff")
class ViewPager2IncrementalMountActivity : FragmentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val viewPager = ViewPager2(this)
    viewPager.adapter = ViewPager2DemoAdapter(this)

    viewPager.registerOnPageChangeCallback(
        object : ViewPager2.OnPageChangeCallback() {
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
                  ViewPager2.SCROLL_STATE_IDLE -> "IDLE"
                  ViewPager2.SCROLL_STATE_DRAGGING -> "DRAGGING"
                  ViewPager2.SCROLL_STATE_SETTLING -> "SETTLING"
                  else -> "UNKNOWN"
                }
            Log.d(TAG, "onPageScrollStateChanged: $stateName")
          }
        },
    )

    setContentView(viewPager)
  }

  companion object {
    private const val TAG = "ViewPager2IM"
  }
}

/** Adapter for ViewPager2 that creates pages with LithoViews configured for incremental mount. */
class ViewPager2DemoAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

  override fun getItemCount(): Int = 5

  override fun createFragment(position: Int): Fragment {
    return ViewPager2PageFragment.newInstance(position)
  }
}

/** Fragment representing a single page in ViewPager2. */
@SuppressLint("BadSuperClassFragment.FbFragment")
class ViewPager2PageFragment : Fragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    val pageIndex = arguments?.getInt(ARG_PAGE_INDEX) ?: 0
    setLithoContent { ViewPager2PageComponent(pageIndex = pageIndex) }
    return getLithoView()
  }

  companion object {
    private const val ARG_PAGE_INDEX = "page_index"

    fun newInstance(pageIndex: Int): ViewPager2PageFragment {
      return ViewPager2PageFragment().apply {
        arguments = Bundle().apply { putInt(ARG_PAGE_INDEX, pageIndex) }
      }
    }
  }
}

/** Component for a single ViewPager2 page showing mount behavior. */
class ViewPager2PageComponent(
    private val pageIndex: Int,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val colors = listOf(
        "#FFCDD2".toColorInt(),
        "#C8E6C9".toColorInt(),
        "#BBDEFB".toColorInt(),
        "#FFF9C4".toColorInt(),
        "#E1BEE7".toColorInt(),
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
                    text = "ViewPager2: IM support enabled by default",
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
                      SimpleItemComponent(
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

/** Simple item component that shows page and item index with mount indicator. */
class SimpleItemComponent(
    private val pageIndex: Int,
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
                    .height(60.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .backgroundColor(bgColor),
    ) {
      child(
          MountLoggerPrimitive2(
              tag = "ViewPager2IM",
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
 * A Primitive component that logs mount/unmount events for ViewPager2.
 *
 * This demonstrates incremental mount - when items scroll into view, they get MOUNTED (gray box
 * appears). When items scroll out of view, they get UNMOUNTED (logged).
 */
class MountLoggerPrimitive2(
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
