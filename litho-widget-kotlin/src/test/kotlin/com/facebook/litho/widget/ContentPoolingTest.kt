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

package com.facebook.litho.kotlin.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.TextView
import com.facebook.litho.Column
import com.facebook.litho.MeasureScope
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.px
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TrackedItemPool
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.MountItemsPool
import com.facebook.rendercore.MountItemsPool.getMountItemPools
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ContentPoolingTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `different components should use different pools`() {

    // Mount an Image component
    lithoViewRule.render {
      Column {
        child(
            TestDrawableMountableComponent(
                drawable = ColorDrawable(Color.RED),
                style = Style.width(100.px).height(100.px),
            ))
      }
    }

    // Should create 1 Pool for TestDrawableMountableComponent
    assertThat(MountItemsPool.getMountItemPools().size).isEqualTo(1)

    // Mount multiple Image components, and a TestTextViewMountableComponent
    val lithoView =
        lithoViewRule
            .render {
              Column {
                child(
                    TestDrawableMountableComponent(
                        drawable = ColorDrawable(Color.RED),
                        style = Style.width(100.px).height(100.px),
                    ))
                child(
                    TestTextViewMountableComponent(
                        style = Style.width(100.px).height(500.px),
                    ))
              }
            }
            .lithoView

    // Should now have 2 Pools; one for the Image, and one for the Vertical Scroll component.
    assertThat(MountItemsPool.getMountItemPools().size).isEqualTo(2)

    // Unmount all content to release all the content to the pools
    lithoView.unmountAllItems()
  }

  @Test
  fun `should use pool size from mountable`() {

    // Mount 40 Image components, and then unmount them all
    lithoViewRule
        .render {
          Column {
            for (i in 1..40) {
              child(
                  TestDrawableMountableComponent(
                      drawable = ColorDrawable(Color.RED),
                      style = Style.width(100.px).height(100.px),
                  ))
            }
          }
        }
        .lithoView
        .unmountAllItems()

    assertThat(getMountItemPools().size).isEqualTo(1)
    assertThat((getMountItemPools()[0] as TrackedItemPool).currentSize).isEqualTo(10)
  }
}

class TestDrawableMountableComponent(val drawable: Drawable, val style: Style? = null) :
    MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    return MountableRenderResult(DrawableMountable(drawable), style)
  }
}

class DrawableMountable(
    val drawable: Drawable,
) : SimpleMountable<Drawable>(RenderType.DRAWABLE) {

  override fun poolSize(): Int = 10

  override fun createContent(context: Context): Drawable {
    return drawable
  }

  override fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult =
      MeasureResult(100, 100)

  override fun mount(c: Context, content: Drawable, layoutData: Any?) = Unit

  override fun unmount(c: Context, content: Drawable, layoutData: Any?) = Unit

  override fun onCreateMountContentPool(): MountItemsPool.ItemPool {
    return TrackedItemPool(this, poolSize())
  }
}

class TestTextViewMountableComponent(val style: Style? = null) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    return MountableRenderResult(TextViewMountable(), style)
  }
}

class TextViewMountable : SimpleMountable<TextView>(RenderType.VIEW) {

  override fun poolSize(): Int = 10

  override fun createContent(context: Context): TextView {
    return TextView(context)
  }

  override fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult =
      MeasureResult(100, 100)

  override fun mount(c: Context, content: TextView, layoutData: Any?) = Unit

  override fun unmount(c: Context, content: TextView, layoutData: Any?) = Unit
}
