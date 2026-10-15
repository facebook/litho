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
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A drawable is mounted inside its component's padding. When a layout from the last pass is reused
 * at a new size, the padding can change with it (a percentage of the width), and the drawable must
 * be mounted where a fresh layout at that size would put it.
 */
@RunWith(LithoTestRunner::class)
class ReusedLayoutDrawableBoundsTest {

  @JvmField @Rule val lithoTestRule: LithoTestRule = LithoTestRule()

  @Test
  fun `a drawable laid out again at a new width is mounted inside its new padding`() {
    val view = lithoTestRule.render(widthPx = 200, heightPx = 300) { tree() }
    assertThat(drawableBounds(view))
        .describedAs("first pass at 200")
        .isEqualTo(Rect(20, 0, 200, 50))

    view.setSizePx(400, 300).measure().layout()

    val fresh = lithoTestRule.render(widthPx = 400, heightPx = 300) { tree() }
    assertThat(drawableBounds(fresh))
        .describedAs("fresh layout at 400")
        .isEqualTo(Rect(40, 0, 400, 50))
    assertThat(drawableBounds(view))
        .describedAs("same tree laid out again at 400")
        .isEqualTo(drawableBounds(fresh))

    view.setSizePx(300, 300).measure().layout()

    assertThat(drawableBounds(view))
        .describedAs("same tree laid out again at 300")
        .isEqualTo(Rect(30, 0, 300, 50))
  }

  // A drawable MountSpec without inter-stage props, so its content unit is kept when its layout
  // is reused; one that writes inter-stage props in @OnBoundsDefined gets a new unit each time.
  private fun tree(): Component = Column {
    child(
        TestDrawableComponent.create(lithoTestRule.context)
            .paddingPercent(YogaEdge.LEFT, 10f)
            .heightPx(50)
            .build(),
    )
  }

  /** The drawable's mount bounds, in its host's coordinates. */
  private fun drawableBounds(view: TestLithoView): Rect {
    val layoutState = checkNotNull(view.committedLayoutState)
    for (i in 0 until layoutState.getMountableOutputCount()) {
      val node = layoutState.getMountableOutputAt(i)
      if ((node.renderUnit as? LithoRenderUnit)?.component is TestDrawableComponent) {
        return node.bounds
      }
    }
    error("no drawable output")
  }
}
