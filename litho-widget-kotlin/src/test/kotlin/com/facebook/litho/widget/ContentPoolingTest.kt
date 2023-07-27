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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.TextView
import com.facebook.litho.Column
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TrackedItemPool
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.MountItemsPool
import com.facebook.rendercore.MountItemsPool.getMountItemPools
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ContentPoolingTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Before
  fun setup() {
    MountItemsPool.clear()
    MountItemsPool.setMountContentPoolFactory(null)
  }

  @Test
  fun `different primitive components should use different pools`() {

    // Mount an Image component
    lithoViewRule.render {
      Column {
        child(
            TestDrawablePrimitiveComponent(
                drawable = ColorDrawable(Color.RED),
                style = Style.width(100.px).height(100.px),
            ))
      }
    }

    // Should create 1 Pool for TestDrawablePrimitiveComponent
    assertThat(MountItemsPool.getMountItemPools().size).isEqualTo(1)

    // Mount multiple Image components, and a TestTextViewPrimitiveComponent
    val lithoView =
        lithoViewRule
            .render {
              Column {
                child(
                    TestDrawablePrimitiveComponent(
                        drawable = ColorDrawable(Color.RED),
                        style = Style.width(100.px).height(100.px),
                    ))
                child(
                    TestTextViewPrimitiveComponent(
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
  fun `should use pool size from primitive`() {

    var createContentInvocationCount = 0

    // Mount 40 Image components, and then unmount them all
    lithoViewRule
        .render {
          Column {
            for (i in 1..40) {
              child(
                  TestDrawablePrimitiveComponent(
                      onCreateContent = { createContentInvocationCount++ },
                      drawable = ColorDrawable(Color.RED),
                      style = Style.width(100.px).height(100.px),
                  ))
            }
          }
        }
        .lithoView
        .unmountAllItems()

    assertThat(getMountItemPools().size).isEqualTo(1)
    assertThat(createContentInvocationCount).isEqualTo(40)

    createContentInvocationCount = 0
    // Mount 40 Image components again to check if pooling works, and then unmount them all
    lithoViewRule
        .render {
          Column {
            for (i in 1..40) {
              child(
                  TestDrawablePrimitiveComponent(
                      onCreateContent = { createContentInvocationCount++ },
                      drawable = ColorDrawable(Color.RED),
                      style = Style.width(100.px).height(100.px),
                  ))
            }
          }
        }
        .lithoView
        .unmountAllItems()

    assertThat(getMountItemPools().size).isEqualTo(1)
    // Create content should be called 30 times because 10 components were in the pool
    assertThat(createContentInvocationCount).isEqualTo(30)
  }

  @Test
  fun `should correctly preallocate primitive component`() {
    MountItemsPool.setMountContentPoolFactory(
        createPoolFactory(TestTextViewPrimitiveComponent.ALLOCATOR.poolSize()))

    // initially there is no pool
    assertThat(getMountItemPools().size).isEqualTo(0)

    // preallocate 40 Text components
    MountItemsPool.prefillMountContentPool(
        lithoViewRule.context.androidContext, 40, TestTextViewPrimitiveComponent.ALLOCATOR)

    // Should create 1 Pool for TestTextViewPrimitiveComponent
    assertThat(getMountItemPools().size).isEqualTo(1)
    // There should be 10 items in the pool
    assertThat((getMountItemPools()[0] as TrackedItemPool).currentSize).isEqualTo(10)
  }
}

// Primitive components

class TestDrawablePrimitiveComponent(
    val onCreateContent: (() -> Unit)? = null,
    val drawable: Drawable,
    val style: Style? = null
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
        mountBehavior =
            MountBehavior(
                DrawableAllocator(poolSize = 10) {
                  onCreateContent?.invoke()
                  drawable
                }) {},
        style = style)
  }
}

// start_prefill_mount_content_pool_primitive_component_example
class TestTextViewPrimitiveComponent(val style: Style? = null) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
        mountBehavior = MountBehavior(ALLOCATOR) {},
        style = style)
  }

  companion object {
    val ALLOCATOR = ViewAllocator(poolSize = 10) { context -> TextView(context) }
  }
}
// end_prefill_mount_content_pool_primitive_component_example

private fun createPoolFactory(poolSize: Int): MountItemsPool.Factory {
  return object : MountItemsPool.Factory {
    override fun createMountContentPool(): MountItemsPool.ItemPool {
      return TrackedItemPool(this, poolSize)
    }
  }
}
