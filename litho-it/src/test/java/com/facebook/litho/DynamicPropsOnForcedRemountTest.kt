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

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.utils.withEqualDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class DynamicPropsOnForcedRemountTest {

  @get:Rule val lithoViewRule: LithoViewRule = LithoViewRule()

  @Test
  fun `ensure that dynamic props still work after forced needs remount`() {
    val dynamicValue = DynamicValue("0001")
    val component = WrapperComponent(dynamicValue)
    val lithoView = lithoViewRule.render(widthPx = 1080, heightPx = 840) { component }
    lithoViewRule.idle()
    LithoAssertions.assertThat(lithoView).hasVisibleText("0001")
    lithoView.detachFromWindow()

    val componentContext = ComponentContext(getApplicationContext() as Context)
    val componentTree = ComponentTree.create(componentContext, component).build()

    lithoView.setComponentTree(componentTree)
    lithoView.markAsNeedsRemount()
    lithoView.attachToWindow()
    LithoAssertions.assertThat(lithoView).hasVisibleText("0001")

    dynamicValue.set("0002")
    LithoAssertions.assertThat(lithoView).hasVisibleText("0002")
  }
}

private class WrapperComponent(private val dynamicValue: DynamicValue<String>) : KComponent() {

  override fun ComponentScope.render(): Component? {
    return TestPrimitiveComponent(tag = dynamicValue)
  }
}

private class TestPrimitiveComponent(
    private val tag: DynamicValue<String>,
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = TestLayoutBehavior,
        mountBehavior =
            MountBehavior(ViewAllocator { context -> TextView(context) }) {
              // simple binding
              bindDynamic(tag, TextView::setText, "0000")
            },
        null)
  }
}

private object TestLayoutBehavior : LayoutBehavior {

  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(
        size =
            if (!sizeConstraints.hasBoundedWidth && !sizeConstraints.hasBoundedHeight) {
              Size(1080, 840)
            } else {
              Size.withEqualDimensions(sizeConstraints)
            })
  }
}
