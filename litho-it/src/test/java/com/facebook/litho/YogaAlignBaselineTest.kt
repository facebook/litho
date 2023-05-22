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
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.yoga.YogaAlign
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class YogaAlignBaselineTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testAlignItemsBaselineNestedTreeColumn() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Row.create(c)
                      .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                      .child(
                          Column.create(c)
                              .widthPx(500)
                              .heightPx(800)
                              .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                              .child(Column.create(c).widthPx(500).heightPx(400).wrapInView()))
                      .alignItems(YogaAlign.BASELINE)
                      .widthPx(1_000)
                      .heightPx(1_000)
                      .build()
            })
    val child0 = lithoView.getChildAt(0)
    val child1Child0 = lithoView.getChildAt(1)
    val child1Child1 = lithoView.getChildAt(2)
    assertThat(child0.left).isEqualTo(0)
    assertThat(child0.top).isEqualTo(0)
    assertThat(child1Child0.left).isEqualTo(500)
    assertThat(child1Child0.top).isEqualTo(300)
    assertThat(child1Child1.left).isEqualTo(500)
    assertThat(child1Child1.top).isEqualTo(600)
  }

  @Test
  fun testAlignItemsBaselineNestedTreeColumnCustomBaselineFunction() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Row.create(c)
                      .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                      .child(
                          Column.create(c)
                              .widthPx(500)
                              .heightPx(800)
                              .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                              .child(Column.create(c).widthPx(500).heightPx(400).wrapInView())
                              .useHeightAsBaseline(true))
                      .alignItems(YogaAlign.BASELINE)
                      .widthPx(1_000)
                      .heightPx(1_000)
                      .build()
            })
    val child0 = lithoView.getChildAt(0)
    val child1Child0 = lithoView.getChildAt(1)
    val child1Child1 = lithoView.getChildAt(2)
    assertThat(child0.left).isEqualTo(0)
    assertThat(child0.top).isEqualTo(200)
    assertThat(child1Child0.left).isEqualTo(500)
    assertThat(child1Child0.top).isEqualTo(0)
    assertThat(child1Child1.left).isEqualTo(500)
    assertThat(child1Child1.top).isEqualTo(300)
  }

  @Test
  fun testAlignItemsBaselineNestedTreeRow() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Row.create(c)
                      .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                      .child(
                          Row.create(c)
                              .widthPx(1_000)
                              .heightPx(800)
                              .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                              .child(Column.create(c).widthPx(500).heightPx(400).wrapInView()))
                      .alignItems(YogaAlign.BASELINE)
                      .widthPx(2_000)
                      .heightPx(2_000)
                      .build()
            })
    val child0 = lithoView.getChildAt(0)
    val child1Child0 = lithoView.getChildAt(1)
    val child1Child1 = lithoView.getChildAt(2)
    assertThat(child0.left).isEqualTo(0)
    assertThat(child0.top).isEqualTo(0)
    assertThat(child1Child0.left).isEqualTo(500)
    assertThat(child1Child0.top).isEqualTo(300)
    assertThat(child1Child1.left).isEqualTo(1_000)
    assertThat(child1Child1.top).isEqualTo(300)
  }

  @Test
  fun testAlignItemsBaselineNestedTreeRowCustomBaselineFunction() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Row.create(c)
                      .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                      .child(
                          Row.create(c)
                              .widthPx(1_000)
                              .heightPx(800)
                              .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                              .child(Column.create(c).widthPx(500).heightPx(400).wrapInView())
                              .useHeightAsBaseline(true))
                      .alignItems(YogaAlign.BASELINE)
                      .widthPx(2_000)
                      .heightPx(2_000)
                      .build()
            })
    val child0 = lithoView.getChildAt(0)
    val child1Child0 = lithoView.getChildAt(1)
    val child1Child1 = lithoView.getChildAt(2)
    assertThat(child0.left).isEqualTo(0)
    assertThat(child0.top).isEqualTo(200)
    assertThat(child1Child0.left).isEqualTo(500)
    assertThat(child1Child0.top).isEqualTo(0)
    assertThat(child1Child1.left).isEqualTo(1_000)
    assertThat(child1Child1.top).isEqualTo(0)
  }

  @Test
  fun testIsReferenceBaselineUsingChildInColumnAsReference() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Row.create(c)
                      .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                      .child(
                          Column.create(c)
                              .widthPx(500)
                              .heightPx(800)
                              .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                              .child(
                                  Column.create(c)
                                      .widthPx(500)
                                      .heightPx(400)
                                      .wrapInView()
                                      .isReferenceBaseline(true)))
                      .alignItems(YogaAlign.BASELINE)
                      .widthPx(1_000)
                      .heightPx(1_000)
                      .build()
            })
    val child0 = lithoView.getChildAt(0)
    val child1Child0 = lithoView.getChildAt(1)
    val child1Child1 = lithoView.getChildAt(2)
    assertThat(child0.left).isEqualTo(0)
    assertThat(child0.top).isEqualTo(100)
    assertThat(child1Child0.left).isEqualTo(500)
    assertThat(child1Child0.top).isEqualTo(0)
    assertThat(child1Child1.left).isEqualTo(500)
    assertThat(child1Child1.top).isEqualTo(300)
  }

  @Test
  fun testIsReferenceBaselineUsingChildInRowAsReference() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Row.create(c)
                      .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                      .child(
                          Row.create(c)
                              .widthPx(1_000)
                              .heightPx(800)
                              .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                              .child(
                                  Column.create(c)
                                      .widthPx(500)
                                      .heightPx(400)
                                      .wrapInView()
                                      .isReferenceBaseline(true)))
                      .alignItems(YogaAlign.BASELINE)
                      .widthPx(2_000)
                      .heightPx(2_000)
                      .build()
            })
    val child0 = lithoView.getChildAt(0)
    val child1Child0 = lithoView.getChildAt(1)
    val child1Child1 = lithoView.getChildAt(2)
    assertThat(child0.left).isEqualTo(0)
    assertThat(child0.top).isEqualTo(0)
    assertThat(child1Child0.left).isEqualTo(500)
    assertThat(child1Child0.top).isEqualTo(200)
    assertThat(child1Child1.left).isEqualTo(1_000)
    assertThat(child1Child1.top).isEqualTo(200)
  }
}
