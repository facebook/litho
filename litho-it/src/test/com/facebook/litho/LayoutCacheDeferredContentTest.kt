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
import android.view.View
import android.view.ViewGroup
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.stateupdates.ComponentWithMeasureCall
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.wrapInView
import com.facebook.rendercore.px
import com.facebook.yoga.YogaDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Content laid out with its size (`RenderWithConstraints`, a `Component.measure` holder) in a
 * subtree the layout cache copies from the last pass is laid out as a fresh layout lays it out.
 */
@RunWith(LithoTestRunner::class)
class LayoutCacheDeferredContentTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  private val defaultRemeasureDeferredNodesInCachedSubtree =
      ComponentsConfiguration.remeasureDeferredNodesInCachedSubtree

  @Before
  fun setUp() {
    ComponentsConfiguration.remeasureDeferredNodesInCachedSubtree = true
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.remeasureDeferredNodesInCachedSubtree =
        defaultRemeasureDeferredNodesInCachedSubtree
  }

  /**
   * After the width shrinks, size-dependent content copied from the cache is resolved under the
   * room it is offered now, not at the size its other branch had.
   */
  @Test
  fun `copied size-dependent content is measured under its new constraints`() {
    val root = {
      Row {
        child(
            Row {
              child(tagged("fixed", 60, 40))
              child(Column(style = Style.width(10.px).height(250.px)))
            },
        )
        child(
            RenderWithConstraints { c ->
              if (c.hasBoundedWidth && c.maxWidth <= 600) Row { child(tagged("narrow", 200, 40)) }
              else Column { child(tagged("wide", 160, 40)) }
            },
        )
      }
    }
    val view = lithoTestRule.render(widthPx = 720, heightPx = 1000) { root() }
    view.setSizePx(240, 1000).measure().layout()

    val fresh = frames(lithoTestRule.render(widthPx = 240, heightPx = 1000) { root() })
    assertThat(fresh["narrow"]).describedAs("fresh").isEqualTo(Rect(62, 0, 240, 40))
    assertThat(frames(view)).isEqualTo(fresh)
  }

  /**
   * A `Component.measure` holder in a row copied whole, offered a width that rounds to its last
   * one, is placed where a fresh layout places it.
   *
   * Yoga compares the sizes it offers a node with a measure function, like the holder, rounded to
   * the pixel grid. Offered 37.65px after 38.4px, the copied holder keeps 38.4px unless it is
   * dirty, and Yoga floors the left edge of such a node, so the RTL content would start at 681,
   * not 682. A plain 80px leaf in its place, compared exactly, is laid out again even when clean.
   */
  @Test
  fun `copied measured content is placed as a fresh layout places it`() {
    val root = {
      Column(style = Style.layoutDirection(YogaDirection.RTL)) {
        child(
            Row {
              child(
                  Row {
                    child(
                        ComponentWithMeasureCall.create(lithoTestRule.context)
                            .component(tagged("measured", 80, 40))
                            .widthSpec(SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY))
                            .heightSpec(SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY))
                            .shouldCacheResult(true)
                            .build(),
                    )
                    child(Column(style = Style.width(10.px).height(250.px)))
                  },
              )
              child(Column(style = Style.widthPercent(100f)) { child(tagged("a", 20, 30)) })
              child(Column(style = Style.widthPercent(100f)) { child(tagged("b", 160, 30)) })
            },
        )
      }
    }
    val view = lithoTestRule.render(widthPx = 1080, heightPx = 1000) { root() }
    view.setSizePx(720, 1000).measure().layout()

    val fresh = frames(lithoTestRule.render(widthPx = 720, heightPx = 1000) { root() })
    assertThat(fresh["measured"]).describedAs("fresh").isEqualTo(Rect(682, 0, 762, 40))
    assertThat(frames(view)).isEqualTo(fresh)
  }

  private fun tagged(tag: String, w: Int, h: Int): Component =
      Column(style = Style.width(w.px).height(h.px).wrapInView().contentDescription(tag))

  /** Each tagged view's frame in the LithoView's coordinates. */
  private fun frames(view: TestLithoView): Map<String, Rect> {
    val out = HashMap<String, Rect>()
    fun walk(v: View) {
      v.contentDescription?.toString()?.let { tag ->
        val rect = Rect(0, 0, v.width, v.height)
        view.lithoView.offsetDescendantRectToMyCoords(v, rect)
        out[tag] = rect
      }
      if (v is ViewGroup) {
        for (i in 0 until v.childCount) walk(v.getChildAt(i))
      }
    }
    walk(view.lithoView)
    return out
  }
}
