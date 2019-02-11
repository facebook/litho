/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.yoga.YogaAlign.BASELINE;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.View;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class YogaAlignBaselineTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testAlignItemsBaselineNestedTreeColumn() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                    .child(
                        Column.create(c)
                            .widthPx(500)
                            .heightPx(800)
                            .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                            .child(Column.create(c).widthPx(500).heightPx(400).wrapInView()))
                    .alignItems(BASELINE)
                    .widthPx(1000)
                    .heightPx(1000)
                    .build();
              }
            });

    final View child0 = lithoView.getChildAt(0);
    final View child1_child0 = lithoView.getChildAt(1);
    final View child1_child1 = lithoView.getChildAt(2);

    assertThat(child0.getLeft()).isEqualTo(0);
    assertThat(child0.getTop()).isEqualTo(0);

    assertThat(child1_child0.getLeft()).isEqualTo(500);
    assertThat(child1_child0.getTop()).isEqualTo(300);

    assertThat(child1_child1.getLeft()).isEqualTo(500);
    assertThat(child1_child1.getTop()).isEqualTo(600);
  }

  @Test
  public void testAlignItemsBaselineNestedTreeColumnCustomBaselineFunction() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                    .child(
                        Column.create(c)
                            .widthPx(500)
                            .heightPx(800)
                            .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                            .child(Column.create(c).widthPx(500).heightPx(400).wrapInView())
                            .useHeightAsBaseline(true))
                    .alignItems(BASELINE)
                    .widthPx(1000)
                    .heightPx(1000)
                    .build();
              }
            });

    final View child0 = lithoView.getChildAt(0);
    final View child1_child0 = lithoView.getChildAt(1);
    final View child1_child1 = lithoView.getChildAt(2);

    assertThat(child0.getLeft()).isEqualTo(0);
    assertThat(child0.getTop()).isEqualTo(200);

    assertThat(child1_child0.getLeft()).isEqualTo(500);
    assertThat(child1_child0.getTop()).isEqualTo(0);

    assertThat(child1_child1.getLeft()).isEqualTo(500);
    assertThat(child1_child1.getTop()).isEqualTo(300);
  }

  @Test
  public void testAlignItemsBaselineNestedTreeRow() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                    .child(
                        Row.create(c)
                            .widthPx(1000)
                            .heightPx(800)
                            .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                            .child(Column.create(c).widthPx(500).heightPx(400).wrapInView()))
                    .alignItems(BASELINE)
                    .widthPx(2000)
                    .heightPx(2000)
                    .build();
              }
            });

    final View child0 = lithoView.getChildAt(0);
    final View child1_child0 = lithoView.getChildAt(1);
    final View child1_child1 = lithoView.getChildAt(2);

    assertThat(child0.getLeft()).isEqualTo(0);
    assertThat(child0.getTop()).isEqualTo(0);

    assertThat(child1_child0.getLeft()).isEqualTo(500);
    assertThat(child1_child0.getTop()).isEqualTo(300);

    assertThat(child1_child1.getLeft()).isEqualTo(1000);
    assertThat(child1_child1.getTop()).isEqualTo(300);
  }

  @Test
  public void testAlignItemsBaselineNestedTreeRowCustomBaselineFunction() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                    .child(
                        Row.create(c)
                            .widthPx(1000)
                            .heightPx(800)
                            .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                            .child(Column.create(c).widthPx(500).heightPx(400).wrapInView())
                            .useHeightAsBaseline(true))
                    .alignItems(BASELINE)
                    .widthPx(2000)
                    .heightPx(2000)
                    .build();
              }
            });

    final View child0 = lithoView.getChildAt(0);
    final View child1_child0 = lithoView.getChildAt(1);
    final View child1_child1 = lithoView.getChildAt(2);

    assertThat(child0.getLeft()).isEqualTo(0);
    assertThat(child0.getTop()).isEqualTo(200);

    assertThat(child1_child0.getLeft()).isEqualTo(500);
    assertThat(child1_child0.getTop()).isEqualTo(0);

    assertThat(child1_child1.getLeft()).isEqualTo(1000);
    assertThat(child1_child1.getTop()).isEqualTo(0);
  }

  @Test
  public void testIsReferenceBaselineUsingChildInColumnAsReference() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
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
                    .alignItems(BASELINE)
                    .widthPx(1000)
                    .heightPx(1000)
                    .build();
              }
            });

    final View child0 = lithoView.getChildAt(0);
    final View child1_child0 = lithoView.getChildAt(1);
    final View child1_child1 = lithoView.getChildAt(2);

    assertThat(child0.getLeft()).isEqualTo(0);
    assertThat(child0.getTop()).isEqualTo(100);

    assertThat(child1_child0.getLeft()).isEqualTo(500);
    assertThat(child1_child0.getTop()).isEqualTo(0);

    assertThat(child1_child1.getLeft()).isEqualTo(500);
    assertThat(child1_child1.getTop()).isEqualTo(300);
  }

  @Test
  public void testIsReferenceBaselineUsingChildInRowAsReference() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .child(Column.create(c).widthPx(500).heightPx(600).wrapInView())
                    .child(
                        Row.create(c)
                            .widthPx(1000)
                            .heightPx(800)
                            .child(Column.create(c).widthPx(500).heightPx(300).wrapInView())
                            .child(
                                Column.create(c)
                                    .widthPx(500)
                                    .heightPx(400)
                                    .wrapInView()
                                    .isReferenceBaseline(true)))
                    .alignItems(BASELINE)
                    .widthPx(2000)
                    .heightPx(2000)
                    .build();
              }
            });

    final View child0 = lithoView.getChildAt(0);
    final View child1_child0 = lithoView.getChildAt(1);
    final View child1_child1 = lithoView.getChildAt(2);

    assertThat(child0.getLeft()).isEqualTo(0);
    assertThat(child0.getTop()).isEqualTo(0);

    assertThat(child1_child0.getLeft()).isEqualTo(500);
    assertThat(child1_child0.getTop()).isEqualTo(200);

    assertThat(child1_child1.getLeft()).isEqualTo(1000);
    assertThat(child1_child1.getTop()).isEqualTo(200);
  }
}
