/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.testing.TestViewComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.yoga.YogaDirection.LTR;
import static com.facebook.yoga.YogaDirection.RTL;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.START;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.shadows.LayoutDirectionViewGroupShadow;
import com.facebook.litho.testing.shadows.LayoutDirectionViewShadow;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@Config(
    manifest = Config.NONE,
    sdk = LOLLIPOP,
    shadows = {LayoutDirectionViewShadow.class, LayoutDirectionViewGroupShadow.class})
@RunWith(LithoTestRunner.class)
public class LayoutDirectionTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
  }

  /**
   * Test that view mount items are laid out in the correct positions for LTR and RTL layout
   * directions.
   */
  @Test
  public void testViewChildrenLayoutDirection() {
    final TestComponent child1 = create(mContext, true, true, false).build();
    final TestComponent child2 = create(mContext, true, true, false).build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .layoutDirection(LTR)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            20,
            10);

    View view1 = lithoView.getChildAt(0);
    View view2 = lithoView.getChildAt(1);

    assertThat(new Rect(view1.getLeft(), view1.getTop(), view1.getRight(), view1.getBottom()))
        .isEqualTo(new Rect(0, 0, 10, 10));

    assertThat(new Rect(view2.getLeft(), view2.getTop(), view2.getRight(), view2.getBottom()))
        .isEqualTo(new Rect(10, 0, 20, 10));

    mountComponent(
        mContext,
        lithoView,
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Row.create(c)
                .layoutDirection(RTL)
                .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                .build();
          }
        },
        20,
        10);

    view1 = lithoView.getChildAt(0);
    view2 = lithoView.getChildAt(1);

    assertThat(new Rect(view1.getLeft(), view1.getTop(), view1.getRight(), view1.getBottom()))
        .isEqualTo(new Rect(10, 0, 20, 10));

    assertThat(new Rect(view2.getLeft(), view2.getTop(), view2.getRight(), view2.getBottom()))
        .isEqualTo(new Rect(0, 0, 10, 10));
  }

  /**
   * Test that drawable mount items are laid out in the correct positions for LTR and RTL layout
   * directions.
   */
  @Test
  public void testDrawableChildrenLayoutDirection() {
    final SimpleMountSpecTester child1 = SimpleMountSpecTester.create(mContext).build();
    final SimpleMountSpecTester child2 = SimpleMountSpecTester.create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .layoutDirection(LTR)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            20,
            10);

    Drawable drawable1 = lithoView.getDrawables().get(0);
    Drawable drawable2 = lithoView.getDrawables().get(1);

    assertThat(drawable1.getBounds()).isEqualTo(new Rect(0, 0, 10, 10));
    assertThat(drawable2.getBounds()).isEqualTo(new Rect(10, 0, 20, 10));

    mountComponent(
        mContext,
        lithoView,
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Row.create(c)
                .layoutDirection(RTL)
                .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                .build();
          }
        },
        20,
        10);

    drawable1 = lithoView.getDrawables().get(0);
    drawable2 = lithoView.getDrawables().get(1);

    assertThat(drawable1.getBounds()).isEqualTo(new Rect(10, 0, 20, 10));
    assertThat(drawable2.getBounds()).isEqualTo(new Rect(0, 0, 10, 10));
  }

  /**
   * Test that layout direction is propagated properly throughout a component hierarchy. This is the
   * default behaviour of layout direction.
   */
  @Test
  public void testInheritLayoutDirection() {
    final SimpleMountSpecTester child1 = SimpleMountSpecTester.create(mContext).build();
    final SimpleMountSpecTester child2 = SimpleMountSpecTester.create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .layoutDirection(RTL)
                    .child(
                        Row.create(c)
                            .wrapInView()
                            .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                            .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10)))
                    .build();
              }
            },
            20,
            10);

    final ComponentHost host = (ComponentHost) lithoView.getChildAt(0);
    final Drawable drawable1 = host.getDrawables().get(0);
    final Drawable drawable2 = host.getDrawables().get(1);

    assertThat(drawable1.getBounds()).isEqualTo(new Rect(10, 0, 20, 10));
    assertThat(drawable2.getBounds()).isEqualTo(new Rect(0, 0, 10, 10));
  }

  /**
   * Test that layout direction is correctly set on child components when it differs from the layout
   * direction of it's parent.
   */
  @Test
  public void testNestedComponentWithDifferentLayoutDirection() {
    final SimpleMountSpecTester child1 = SimpleMountSpecTester.create(mContext).build();
    final SimpleMountSpecTester child2 = SimpleMountSpecTester.create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .layoutDirection(RTL)
                    .child(
                        Row.create(c)
                            .layoutDirection(LTR)
                            .wrapInView()
                            .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                            .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10)))
                    .build();
              }
            },
            20,
            10);

    final ComponentHost host = (ComponentHost) lithoView.getChildAt(0);
    final Drawable drawable1 = host.getDrawables().get(0);
    final Drawable drawable2 = host.getDrawables().get(1);

    assertThat(drawable1.getBounds()).isEqualTo(new Rect(0, 0, 10, 10));
    assertThat(drawable2.getBounds()).isEqualTo(new Rect(10, 0, 20, 10));
  }

  /**
   * Test that margins on START and END are correctly applied to the correct side of the component
   * depending upon the applied layout direction.
   */
  @Test
  public void testMargin() {
    final SimpleMountSpecTester child = SimpleMountSpecTester.create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .layoutDirection(LTR)
                    .child(
                        Wrapper.create(c)
                            .delegate(child)
                            .widthPx(10)
                            .heightPx(10)
                            .marginPx(START, 10)
                            .marginPx(END, 20))
                    .build();
              }
            },
            40,
            10);

    Drawable drawable = lithoView.getDrawables().get(0);
    assertThat(drawable.getBounds()).isEqualTo(new Rect(10, 0, 20, 10));

    mountComponent(
        mContext,
        lithoView,
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .layoutDirection(RTL)
                .child(
                    Wrapper.create(c)
                        .delegate(child)
                        .widthPx(10)
                        .heightPx(10)
                        .marginPx(START, 10)
                        .marginPx(END, 20))
                .build();
          }
        },
        40,
        10);

    drawable = lithoView.getDrawables().get(0);
    assertThat(drawable.getBounds()).isEqualTo(new Rect(20, 0, 30, 10));
  }

  /**
   * Test that paddings on START and END are correctly applied to the correct side of the component
   * depending upon the applied layout direction.
   */
  @Test
  public void testPadding() {
    final SimpleMountSpecTester child = SimpleMountSpecTester.create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .layoutDirection(LTR)
                    .paddingPx(START, 10)
                    .paddingPx(END, 20)
                    .child(Wrapper.create(c).delegate(child).widthPx(10).heightPx(10))
                    .build();
              }
            },
            40,
            10);

    Drawable drawable = lithoView.getDrawables().get(0);
    assertThat(drawable.getBounds()).isEqualTo(new Rect(10, 0, 20, 10));

    mountComponent(
        mContext,
        lithoView,
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .layoutDirection(RTL)
                .paddingPx(START, 10)
                .paddingPx(END, 20)
                .child(Wrapper.create(c).delegate(child).widthPx(10).heightPx(10))
                .build();
          }
        },
        40,
        10);

    drawable = lithoView.getDrawables().get(0);
    assertThat(drawable.getBounds()).isEqualTo(new Rect(20, 0, 30, 10));
  }

  /**
   * Tests to make sure the layout direction set on the component tree is correctly propagated to
   * mounted views.
   */
  @Test
  public void testLayoutDirectionPropagation() {
    final TestComponent child = create(mContext).build();

    LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).layoutDirection(RTL).child(child).build();
              }
            });

    final View childView = lithoView.getChildAt(0);
    assertThat(childView.getLayoutDirection()).isEqualTo(LAYOUT_DIRECTION_RTL);
  }
}
