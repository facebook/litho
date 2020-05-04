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

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;
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
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.assertj.LithoViewAssert;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
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

  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Before
  public void setup() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).widthPx(100).heightPx(100).build();
          }
        };

    mLithoView = new LithoView(getApplicationContext());
    mLithoView.setComponent(component);
  }

  @After
  public void tearDown() {
    ComponentsConfiguration.isDebugModeEnabled = ComponentsConfiguration.IS_INTERNAL_BUILD;
  }

  @Test
  public void measureBeforeBeingAttached() {
    mLithoView.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(0, UNSPECIFIED));
    mLithoView.layout(0, 0, mLithoView.getMeasuredWidth(), mLithoView.getMeasuredHeight());

    // View got measured.
    assertThat(mLithoView.getMeasuredWidth()).isGreaterThan(0);
    assertThat(mLithoView.getMeasuredHeight()).isGreaterThan(0);

    // Attaching will automatically mount since we already have a layout fitting our size.
    ShadowView shadow = shadowOf(mLithoView);
    shadow.callOnAttachedToWindow();

    assertThat(getInternalMountItems(mLithoView)).hasSize(2);
  }

  private static long[] getInternalMountItems(LithoView lithoView) {
    MountState mountState = Whitebox.getInternalState(lithoView, "mMountState");
    return Whitebox.getInternalState(mountState, "mLayoutOutputsIds");
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

    nullLithoView.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(0, UNSPECIFIED));
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
    mLithoView.measure(makeMeasureSpec(width, EXACTLY), makeMeasureSpec(height, EXACTLY));

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
            return TestDrawableComponent.create(c).widthPercent(100).heightPx(100).build();
          }
        };

    mLithoView = new LithoView(getApplicationContext());
    mLithoView.setComponent(component);

    mLithoView.setLayoutParams(new ViewGroup.LayoutParams(0, 200));
    mLithoView.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(200, EXACTLY));
    mLithoView.layout(0, 0, mLithoView.getMeasuredWidth(), mLithoView.getMeasuredHeight());

    // View got measured.
    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(0);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(200);

    // Attaching will not mount anything as we have no width.
    ShadowView shadow = shadowOf(mLithoView);
    shadow.callOnAttachedToWindow();

    assertThat(getInternalMountItems(mLithoView)).isNull();
  }

  /** This verifies that the width is correct with at most layout params. */
  @Test
  public void measureWithAtMostLayoutParams() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).widthPercent(50).heightPercent(10).build();
          }
        };

    mLithoView = new LithoView(getApplicationContext());
    mLithoView.setComponent(component);

    mLithoView.setLayoutParams(
        new RecyclerViewLayoutManagerOverrideParams(
            SizeSpec.makeSizeSpec(100, SizeSpec.AT_MOST),
            SizeSpec.makeSizeSpec(200, SizeSpec.AT_MOST)));
    mLithoView.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(0, UNSPECIFIED));
    mLithoView.layout(0, 0, mLithoView.getMeasuredWidth(), mLithoView.getMeasuredHeight());

    // View got measured.
    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(50);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(20);

    // Attaching will automatically mount since we already have a layout fitting our size.
    ShadowView shadow = shadowOf(mLithoView);
    shadow.callOnAttachedToWindow();

    assertThat(getInternalMountItems(mLithoView)).hasSize(2);
  }

  @Test
  public void testCorrectsDoubleMeasureBug() {
    mLithoView = setupLithoViewForDoubleMeasureTest(411, 2.625f, 1080);
    mLithoView.measure(makeMeasureSpec(1079, EXACTLY), makeMeasureSpec(100, EXACTLY));

    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(1080);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(100);
  }

  @Test
  public void testCorrectsDoubleMeasureBugWithAtMost() {
    mLithoView = setupLithoViewForDoubleMeasureTest(411, 2.625f, 1080);
    mLithoView.measure(makeMeasureSpec(1079, AT_MOST), makeMeasureSpec(100, EXACTLY));

    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(1080);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(100);
  }

  @Test
  public void testNoCorrectionWhenBugIsNotMatched() {
    mLithoView = setupLithoViewForDoubleMeasureTest(411, 2f, 1080);
    mLithoView.measure(makeMeasureSpec(1079, EXACTLY), makeMeasureSpec(100, EXACTLY));

    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(1079);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(100);
  }

  @Test
  public void testNoCorrectionWhenBugIsNotMatched2() {
    mLithoView = setupLithoViewForDoubleMeasureTest(411, 2.625f, 1080);
    mLithoView.measure(makeMeasureSpec(1078, EXACTLY), makeMeasureSpec(100, EXACTLY));

    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(1078);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(100);
  }

  @Test
  public void testMeasureDoesNotComputeLayoutStateWhenSpecsAreExact() {
    mLithoView = new LithoView(getApplicationContext());
    mLithoView.setComponent(TestDrawableComponent.create(mLithoView.getComponentContext()).build());
    mLithoView.measure(makeMeasureSpec(100, EXACTLY), makeMeasureSpec(100, EXACTLY));

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
        TestDrawableComponent.create(mLithoView.getComponentContext()).heightPx(100).build());
    mLithoView.measure(makeMeasureSpec(100, EXACTLY), makeMeasureSpec(100, AT_MOST));

    assertThat(mLithoView.getMeasuredWidth()).isEqualTo(100);
    assertThat(mLithoView.getMeasuredHeight()).isEqualTo(100);
    assertThat(mLithoView.getComponentTree().getMainThreadLayoutState()).isNotNull();
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
