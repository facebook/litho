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

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.widget.FrameLayout;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.utils.IncrementalMountUtils;
import com.facebook.litho.widget.LithoScrollView;
import com.facebook.litho.widget.VerticalScroll;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class SelfManagingLithoViewTest {
  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Test
  public void lithoViewWithTranslatedParent_mountsOnlyVisibleItems() {
    // This test is only relevant for self managing litho-views.
    if (!ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return;
    }

    final Context context = mLegacyLithoViewRule.context.getAndroidContext();

    // The LithoView's parent's parent. 100x100 with 30 translationY
    final FrameLayout parent1 = new FrameLayout(context);
    parent1.setTop(0);
    parent1.setBottom(100);
    parent1.setLeft(0);
    parent1.setRight(100);
    parent1.setTranslationY(30);

    // The LithoView's parent. 100x100 with 20 translationY (total 50 for LV)
    final FrameLayout parent2 = new FrameLayout(context);
    parent2.setTop(0);
    parent2.setBottom(100);
    parent2.setLeft(0);
    parent2.setRight(100);
    parent2.setTranslationY(20);

    parent1.addView(parent2);

    final LithoView lithoView = mLegacyLithoViewRule.getLithoView();
    parent2.addView(lithoView, 100, 100);

    // Create a vertical scroll spec with 10 children.
    // 5 of these children will be outside the parents bounds due to translation.
    mLegacyLithoViewRule
        .setRoot(createVerticalScrollWithChildren(10))
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
        .attachToWindow()
        .measure()
        .layout();

    // Assert that the vertical scroll is mounted.
    assertThat(lithoView.getMountItemCount()).isEqualTo(1);

    final List<LithoView> childLithoViews = lithoView.getChildLithoViewsFromCurrentlyMountedItems();

    // Assert that there is a single child litho view containing the vertical scroll content
    assertThat(childLithoViews.size()).isEqualTo(1);

    final LithoView scrollerLithoView = childLithoViews.get(0);

    // Assert that only 5 children are mounted
    assertThat(scrollerLithoView.getMountItemCount()).isEqualTo(5);

    // Reset translation to 0
    parent1.setTranslationY(0);
    parent2.setTranslationY(0);

    // Inform of manual position change
    IncrementalMountUtils.incrementallyMountLithoViews(parent1, true);

    // Assert that all 10 children are mounted
    assertThat(scrollerLithoView.getMountItemCount()).isEqualTo(10);

    // clean up
    parent1.removeAllViews();
    parent2.removeAllViews();
  }

  @Test
  public void whenScrolling_lithoViewDoesNotUpdateUntilViewTreeObserverFires() {
    // This test is only relevant for self managing litho-views.
    if (!ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return;
    }

    mLegacyLithoViewRule
        .setRoot(createVerticalScrollWithChildren(10))
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(49, SizeSpec.EXACTLY))
        .attachToWindow()
        .measure()
        .layout();

    final LithoView lithoView = mLegacyLithoViewRule.getLithoView();

    // Assert that the vertical scroll is mounted.
    assertThat(lithoView.getMountItemCount()).isEqualTo(1);

    final LithoScrollView scrollView = (LithoScrollView) lithoView.getMountItemAt(0).getContent();
    final LithoView nestedLithoView = (LithoView) scrollView.getChildAt(0);

    // Assert that only 5 children are mounted
    assertThat(nestedLithoView.getMountItemCount()).isEqualTo(5);

    // Scroll by 5, allowing room for a 6th child.
    scrollView.scrollBy(0, 5);

    // VTO did not fire yet, so there should still only be 5 mounted children
    assertThat(nestedLithoView.getMountItemCount()).isEqualTo(5);

    // Fire the VTO
    mLegacyLithoViewRule.dispatchGlobalLayout();

    // Assert that there are now 6 mounted children.
    assertThat(nestedLithoView.getMountItemCount()).isEqualTo(6);
  }

  @Test
  public void whenOffsetTopAndBottom_IncrementalMountUtilsShouldEnsureMount() {
    // This test is only relevant for self managing litho-views.
    if (!ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;
    final Context context = c.getAndroidContext();

    final Column.Builder builder = Column.create(c);

    // create 10 children vertically stacked (each with 10 height).
    for (int i = 0; i < 10; i++) {
      builder.child(createVerticalScrollChild());
    }

    final FrameLayout parent1 = new FrameLayout(context);
    parent1.setTop(0);
    parent1.setBottom(100);
    parent1.setLeft(0);
    parent1.setRight(100);

    final FrameLayout parent2 = new FrameLayout(context);
    parent2.setTop(0);
    parent2.setBottom(100);
    parent2.setLeft(0);
    parent2.setRight(100);

    parent1.addView(parent2);

    final LithoView lithoView = mLegacyLithoViewRule.getLithoView();
    parent2.addView(lithoView, 100, 100);

    mLegacyLithoViewRule
        .setRoot(builder.build())
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
        .attachToWindow()
        .measure()
        .layout();

    // Assert that all 10 children are mounted as expected.
    assertThat(lithoView.getMountItemCount()).isEqualTo(10);

    // Offset the middle parent by 51 (vertically), making only 5 children on-screen.
    parent2.offsetTopAndBottom(51);

    // Use IncrementalMountUtils to inform that a manual position change occurred.
    IncrementalMountUtils.incrementallyMountLithoViews(parent1, true);

    // Assert that now, only 5 children are mounted.
    assertThat(lithoView.getMountItemCount()).isEqualTo(5);
  }

  private Component createVerticalScrollWithChildren(int count) {
    final ComponentContext c = mLegacyLithoViewRule.context;

    final Column.Builder scrollContent = Column.create(c);

    for (int i = 0; i < count; i++) {
      scrollContent.child(createVerticalScrollChild());
    }

    return VerticalScroll.create(c)
        .incrementalMountEnabled(true)
        .childComponent(scrollContent.build())
        .build();
  }

  private Component createVerticalScrollChild() {
    final ComponentContext c = mLegacyLithoViewRule.context;
    return Row.create(c).widthPx(10).heightPx(10).backgroundColor(0xFFFF0000).build();
  }
}
