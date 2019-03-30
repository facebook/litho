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

import static com.facebook.litho.Column.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.TOP;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.drawable.ComparableDrawableWrapper;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.ViewGroupWithLithoViewChildren;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class MountStateViewTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testViewPaddingAndBackground() {
    final int color = 0xFFFF0000;
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(
                        TestViewComponent.create(c)
                            .paddingPx(LEFT, 5)
                            .paddingPx(TOP, 6)
                            .paddingPx(RIGHT, 7)
                            .paddingPx(BOTTOM, 8)
                            .backgroundColor(color))
                    .build();
              }
            });

    final View child = lithoView.getChildAt(0);
    Drawable background = child.getBackground();

    if (background instanceof ComparableDrawableWrapper) {
      background = ((ComparableDrawableWrapper) background).getWrappedDrawable();
    }

    assertThat(child.getPaddingLeft()).isEqualTo(5);
    assertThat(child.getPaddingTop()).isEqualTo(6);
    assertThat(child.getPaddingRight()).isEqualTo(7);
    assertThat(child.getPaddingBottom()).isEqualTo(8);
    assertThat(background).isInstanceOf(ColorDrawable.class);
    assertThat(((ColorDrawable) background).getColor()).isEqualTo(color);
  }

  @Test
  public void testSettingZeroPaddingOverridesDefaultBackgroundPadding() {
    final ComponentContext c =
        new ComponentContext(
            new ContextThemeWrapper(
                RuntimeEnvironment.application, R.style.TestTheme_BackgroundWithPadding));

    final LithoView lithoView =
        mountComponent(c, TestViewComponent.create(c).paddingPx(ALL, 0).build());

    final View child = lithoView.getChildAt(0);

    assertThat(child.getPaddingLeft()).isZero();
    assertThat(child.getPaddingTop()).isZero();
    assertThat(child.getPaddingRight()).isZero();
    assertThat(child.getPaddingBottom()).isZero();
  }

  @Test
  public void testSettingOneSidePaddingClearsTheRest() {
    final ComponentContext c =
        new ComponentContext(
            new ContextThemeWrapper(
                RuntimeEnvironment.application, R.style.TestTheme_BackgroundWithPadding));

    final LithoView lithoView =
        mountComponent(c, TestViewComponent.create(c).paddingPx(LEFT, 12).build());

    final View child = lithoView.getChildAt(0);

    assertThat(child.getPaddingLeft()).isEqualTo(12);
    assertThat(child.getPaddingTop()).isZero();
    assertThat(child.getPaddingRight()).isZero();
    assertThat(child.getPaddingBottom()).isZero();
  }

  @Test
  public void testComponentDeepUnmount() {
    final TestComponent testComponent1 = TestDrawableComponent.create(mContext).build();
    final TestComponent testComponent2 = TestDrawableComponent.create(mContext).build();

    final Component mountedTestComponent1 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Wrapper.create(c).delegate(testComponent1).widthPx(10).heightPx(10))
                .build();
          }
        };
    final Component mountedTestComponent2 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Wrapper.create(c).delegate(testComponent2).widthPx(10).heightPx(10))
                .build();
          }
        };
    final LithoView child1 = mountComponent(mContext, mountedTestComponent1, true);
    final LithoView child2 = mountComponent(mContext, mountedTestComponent2, true);

    assertThat(testComponent1.isMounted()).isTrue();
    assertThat(testComponent2.isMounted()).isTrue();

    final ViewGroupWithLithoViewChildren viewGroup =
        new ViewGroupWithLithoViewChildren(mContext.getAndroidContext());
    removeParent(child1);
    removeParent(child2);
    viewGroup.addView(child1);
    viewGroup.addView(child2);

    final LithoView parentView =
        mountComponent(
            mContext, TestViewComponent.create(mContext).testView(viewGroup).build(), true);

    ComponentTestHelper.unmountComponent(parentView);

    assertThat(testComponent1.isMounted()).isFalse();
    assertThat(testComponent2.isMounted()).isFalse();
  }

  private void removeParent(View child) {
    final ViewGroup parent = (ViewGroup) child.getParent();
    parent.removeView(child);
  }
}
