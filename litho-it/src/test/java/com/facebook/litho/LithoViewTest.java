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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.testing.MeasureSpecTestingUtilsKt.atMost;
import static com.facebook.litho.testing.MeasureSpecTestingUtilsKt.exactly;
import static com.facebook.litho.testing.MeasureSpecTestingUtilsKt.unspecified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoStatsRule;
import com.facebook.litho.testing.assertj.LithoViewAssert;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowView;

@RunWith(LithoTestRunner.class)
public class LithoViewTest {
  private LithoView mLithoView;
  private Component mInitialComponent;

  @Rule public ExpectedException mExpectedException = ExpectedException.none();
  @Rule public LithoStatsRule mLithoStatsRule = new LithoStatsRule();

  @Before
  public void setup() {
    mInitialComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).widthPx(100).heightPx(100).build();
          }
        };

    mLithoView = new LithoView(getApplicationContext());
    mLithoView.setComponent(mInitialComponent);
  }

  @After
  public void tearDown() {
    ComponentsConfiguration.isDebugModeEnabled = ComponentsConfiguration.IS_INTERNAL_BUILD;
  }

  @Test
  public void measureBeforeBeingAttached() {
    mLithoView.measure(unspecified(), unspecified());
    mLithoView.layout(0, 0, mLithoView.getMeasuredWidth(), mLithoView.getMeasuredHeight());

    // View got measured.
    assertThat(mLithoView.getMeasuredWidth()).isGreaterThan(0);
    assertThat(mLithoView.getMeasuredHeight()).isGreaterThan(0);

    // Attaching will automatically mount since we already have a layout fitting our size.
    ShadowView shadow = shadowOf(mLithoView);
    shadow.callOnAttachedToWindow();

    assertThat(getInternalMountItems(mLithoView)).isEqualTo(2);
  }

  private static int getInternalMountItems(LithoView lithoView) {
    return lithoView.getMountDelegateTarget().getMountItemCount();
  }

  @Test
  public void testNullLithoViewDimensions() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return null;
          }
        };

    LithoView nullLithoView = new LithoView(getApplicationContext());
    nullLithoView.setComponent(component);

    nullLithoView.measure(unspecified(), unspecified());
    nullLithoView.layout(0, 0, nullLithoView.getMeasuredWidth(), nullLithoView.getMeasuredHeight());

    LithoViewAssert.assertThat(nullLithoView).hasMeasuredWidthOf(0).hasMeasuredHeightOf(0);
  }

  @Test
  public void testSuppressMeasureComponentTree() {
    final ComponentTree mockComponentTree = mock(ComponentTree.class);
    final int width = 240;
    final int height = 400;

    mLithoView.setComponentTree(mockComponentTree);
    mLithoView.suppressMeasureComponentTree(true);
    mLithoView.measure(exactly(width), exactly(height));

    verify(mockComponentTree, never()).measure(anyInt(), anyInt(), (int[]) any(), anyBoolean());
    LithoViewAssert.assertThat(mLithoView).hasMeasuredWidthOf(width).hasMeasuredHeightOf(height);
  }

  @Test
  public void testDontThrowWhenLayoutStateIsNull() {
    final ComponentTree mockComponentTree = mock(ComponentTree.class);

    mLithoView.setComponentTree(mockComponentTree);
    mLithoView.requestLayout();
    mLithoView.notifyVisibleBoundsChanged();
  }

  /** This verifies that the width is 0 with normal layout params. */
  @Test
  public void measureWithLayoutParams() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).widthPercent(100).heightPx(100).build();
          }
        };

    mLithoView = new LithoView(getApplicationContext());
    mLithoView.setComponent(component);

    mLithoView.setLayoutParams(new ViewGroup.LayoutParams(0, 200));
    mLithoView.measure(unspecified(), exactly(200));
    mLithoView.layout(0, 0, mLithoView.getMeasuredWidth(), mLithoView.getMeasuredHeight());

    // View got measured.
    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(0);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(200);

    // Attaching will not mount anything as we have no width.
    ShadowView shadow = shadowOf(mLithoView);
    shadow.callOnAttachedToWindow();

    // With no volume, ensure the component is not mounted.
    // When IM is blocked when rect is empty - nothing is mounted, so we expect 0 items.
    // When IM continues when rect is empty - the root host is mounted, so we expect 1 item.
    final int totalExpectedMountedItems =
        ComponentsConfiguration.shouldContinueIncrementalMountWhenVisibileRectIsEmpty ? 1 : 0;
    assertThat(getInternalMountItems(mLithoView)).isEqualTo(totalExpectedMountedItems);
  }

  /** This verifies that the width is correct with at most layout params. */
  @Test
  public void measureWithAtMostLayoutParams() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).widthPercent(50).heightPercent(10).build();
          }
        };

    mLithoView = new LithoView(getApplicationContext());
    mLithoView.setComponent(component);

    mLithoView.setLayoutParams(
        new RecyclerViewLayoutManagerOverrideParams(
            SizeSpec.makeSizeSpec(100, SizeSpec.AT_MOST),
            SizeSpec.makeSizeSpec(200, SizeSpec.AT_MOST)));
    mLithoView.measure(unspecified(), unspecified());
    mLithoView.layout(0, 0, mLithoView.getMeasuredWidth(), mLithoView.getMeasuredHeight());

    // View got measured.
    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(50);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(20);

    // Attaching will automatically mount since we already have a layout fitting our size.
    ShadowView shadow = shadowOf(mLithoView);
    shadow.callOnAttachedToWindow();

    assertThat(getInternalMountItems(mLithoView)).isEqualTo(2);
  }

  @Test
  public void testMeasureDoesNotComputeLayoutStateWhenSpecsAreExact() {
    mLithoView = new LithoView(getApplicationContext());
    mLithoView.setComponent(SimpleMountSpecTester.create(mLithoView.getComponentContext()).build());
    mLithoView.measure(exactly(100), exactly(100));

    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(100);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(100);
    assertThat(mLithoView.getComponentTree().getMainThreadLayoutState()).isNull();

    mLithoView.layout(0, 0, 50, 50);

    final LayoutState layoutState = mLithoView.getComponentTree().getMainThreadLayoutState();
    assertThat(layoutState).isNotNull();
    assertThat(layoutState.isCompatibleSize(50, 50)).isTrue();
  }

  @Test
  public void testMeasureComputesLayoutStateWhenSpecsAreNotExact() {
    mLithoView = new LithoView(getApplicationContext());
    mLithoView.setComponent(
        SimpleMountSpecTester.create(mLithoView.getComponentContext()).heightPx(100).build());
    mLithoView.measure(exactly(100), atMost(100));

    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(100);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(100);
    assertThat(mLithoView.getComponentTree().getMainThreadLayoutState()).isNotNull();
  }

  @Test
  public void forceLayout_whenForceLayoutIsSet_recomputesLayout() {
    mLithoView.measure(exactly(100), atMost(100));
    mLithoView.forceRelayout();
    mLithoView.measure(exactly(100), atMost(100));

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs("Should force layout.")
        .isEqualTo(2);

    mLithoView.measure(exactly(100), atMost(100));

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs("Should only force layout one time.")
        .isEqualTo(2);
  }

  @Test
  public void forceLayout_whenForceLayoutIsNotSet_doesNotRecomputeLayout() {
    mLithoView.measure(exactly(100), atMost(100));
    mLithoView.measure(exactly(100), atMost(100));
    mLithoView.measure(exactly(100), atMost(100));

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs("Should only compute the first layout as other layouts are not forced.")
        .isEqualTo(1);
  }

  @Test
  public void forceLayout_whenForceLayoutIsSetAndHasExactMeasurements_recomputesLayout() {
    mLithoView.measure(exactly(100), exactly(100));
    mLithoView.layout(0, 0, 100, 100);

    mLithoView.forceRelayout();

    mLithoView.measure(exactly(100), exactly(100));
    mLithoView.layout(0, 0, 100, 100);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs("Should force layout.")
        .isEqualTo(2);

    mLithoView.measure(exactly(100), exactly(100));
    mLithoView.layout(0, 0, 100, 100);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs("Should only force layout one time.")
        .isEqualTo(2);
  }

  @Test
  public void forceLayout_whenForceLayoutIsNotSetAndHasExactMeasurements_doesNotRecomputeLayout() {
    mLithoView.measure(exactly(100), exactly(100));
    mLithoView.layout(0, 0, 100, 100);

    mLithoView.measure(exactly(100), exactly(100));
    mLithoView.layout(0, 0, 100, 100);

    mLithoView.measure(exactly(100), exactly(100));
    mLithoView.layout(0, 0, 100, 100);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs("Should only force layout one time.")
        .isEqualTo(1);
  }

  @Test
  public void getRootComponent_returnsRootComponentWhenSet_viaSetComponent() {
    assertThat(mLithoView.getRootComponent()).isEqualTo(mInitialComponent);

    final Component newRootComponent =
        SimpleMountSpecTester.create(mLithoView.getComponentContext()).heightPx(12345).build();
    mLithoView.setComponent(newRootComponent);

    assertThat(mLithoView.getRootComponent()).isEqualTo(newRootComponent);
  }

  @Test
  public void getRootComponent_returnsRootComponentWhenSet_viaSetComponentTree() {
    assertThat(mLithoView.getRootComponent()).isEqualTo(mInitialComponent);

    final ComponentContext c = mLithoView.getComponentContext();
    final Component newRootComponent = SimpleMountSpecTester.create(c).heightPx(12345).build();
    mLithoView.setComponentTree(ComponentTree.create(c, newRootComponent).build());

    assertThat(mLithoView.getRootComponent()).isEqualTo(newRootComponent);
  }

  @Test
  public void getRootComponent_returnsNullComponentWhenNoComponentSet() {
    mLithoView = new LithoView(getApplicationContext());

    assertThat(mLithoView.getRootComponent()).isNull();
  }

  @Test
  public void getRootComponent_returnsNullWhenNoComponent_viaSetComponentTree() {
    assumeThat(mLithoView.getRootComponent()).isEqualTo(mInitialComponent);

    mLithoView.setComponentTree(null);
    // Measure + layout is necessary to ensure async operations have finished come assertion point
    mLithoView.measure(unspecified(), unspecified());
    mLithoView.layout(0, 0, 50, 50);

    assertThat(mLithoView.getRootComponent()).isNull();
  }

  @Test
  public void getRootComponent_returnsEmptyComponentWhenNoComponent_viaSetComponent() {
    assumeThat(mLithoView.getRootComponent()).isEqualTo(mInitialComponent);

    mLithoView.setComponent(null);
    // Measure + layout is necessary to ensure async operations have finished come assertion point
    mLithoView.measure(unspecified(), unspecified());
    mLithoView.layout(0, 0, 50, 50);

    assertThat(mLithoView.getRootComponent()).isInstanceOf(EmptyComponent.class);
  }

  private LithoView setupLithoViewForDoubleMeasureTest(
      int screenWidthDp, float density, int screenWidthPx) {
    final Context context = spy(new ContextWrapper(getApplicationContext()));
    final Resources resources = spy(context.getResources());

    doReturn(resources).when(context).getResources();

    final Configuration configuration = new Configuration();
    configuration.setTo(resources.getConfiguration());

    final DisplayMetrics displayMetrics = new DisplayMetrics();
    displayMetrics.setTo(resources.getDisplayMetrics());

    doReturn(configuration).when(resources).getConfiguration();
    doReturn(displayMetrics).when(resources).getDisplayMetrics();

    configuration.screenWidthDp = screenWidthDp;
    displayMetrics.density = density;
    displayMetrics.widthPixels = screenWidthPx;

    return new LithoView(context);
  }

  private static class RecyclerViewLayoutManagerOverrideParams extends ViewGroup.LayoutParams
      implements LithoView.LayoutManagerOverrideParams {

    private final int mWidthMeasureSpec;
    private final int mHeightMeasureSpec;

    private RecyclerViewLayoutManagerOverrideParams(int widthMeasureSpec, int heightMeasureSpec) {
      super(WRAP_CONTENT, WRAP_CONTENT);
      mWidthMeasureSpec = widthMeasureSpec;
      mHeightMeasureSpec = heightMeasureSpec;
    }

    @Override
    public int getWidthMeasureSpec() {
      return mWidthMeasureSpec;
    }

    @Override
    public int getHeightMeasureSpec() {
      return mHeightMeasureSpec;
    }

    @Override
    public boolean hasValidAdapterPosition() {
      return false;
    }
  }
}
