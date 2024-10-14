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
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class MountStateBoundsTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testMountedDrawableBounds() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  TestDrawableComponent.create(c).widthPx(10).heightPx(10).build()
            })
    assertThat(lithoView.drawables[0].bounds).isEqualTo(Rect(0, 0, 10, 10))
  }

  @Test
  fun testMountedViewBounds() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  TestViewComponent.create(c).widthPx(10).heightPx(10).build()
            })
    val mountedView = lithoView.getChildAt(0)
    assertThat(Rect(mountedView.left, mountedView.top, mountedView.right, mountedView.bottom))
        .isEqualTo(Rect(0, 0, 10, 10))
  }

  @Test
  fun testInnerComponentHostBounds() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .child(
                          Column.create(c)
                              .widthPx(20)
                              .heightPx(20)
                              .wrapInView()
                              .child(TestDrawableComponent.create(c).widthPx(10).heightPx(10)))
                      .build()
            })
    val host = lithoView.getChildAt(0) as ComponentHost
    assertThat(host.drawables[0].bounds).isEqualTo(Rect(0, 0, 10, 10))
    assertThat(Rect(host.left, host.top, host.right, host.bottom)).isEqualTo(Rect(0, 0, 20, 20))
  }

  @Test
  fun testDoubleInnerComponentHostBounds() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .alignItems(YogaAlign.FLEX_END)
                      .justifyContent(YogaJustify.FLEX_END)
                      .child(
                          Column.create(c)
                              .widthPx(100)
                              .heightPx(100)
                              .paddingPx(YogaEdge.ALL, 20)
                              .wrapInView()
                              .child(
                                  Column.create(c)
                                      .widthPx(60)
                                      .heightPx(60)
                                      .wrapInView()
                                      .child(
                                          TestDrawableComponent.create(c)
                                              .widthPx(20)
                                              .heightPx(20)
                                              .marginPx(YogaEdge.ALL, 20))))
                      .build()
            },
            200,
            200)
    val host = lithoView.getChildAt(0) as ComponentHost
    val nestedHost = host.getChildAt(0) as ComponentHost
    assertThat(Rect(host.left, host.top, host.right, host.bottom))
        .isEqualTo(Rect(100, 100, 200, 200))
    assertThat(nestedHost.drawables[0].bounds).isEqualTo(Rect(20, 20, 40, 40))
    assertThat(Rect(nestedHost.left, nestedHost.top, nestedHost.right, nestedHost.bottom))
        .isEqualTo(Rect(20, 20, 80, 80))
  }
}
