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

import static com.facebook.litho.LifecycleStep.ON_MOUNT;
import static com.facebook.litho.LifecycleStep.ON_UNMOUNT;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.TestViewComponent.create;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.sections.widget.RecyclerConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.ViewGroupWithLithoViewChildren;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.widget.LithoViewFactory;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import com.facebook.litho.widget.MountSpecLifecycleTesterDrawable;
import com.facebook.litho.widget.SimpleMountSpecTester;
import com.facebook.litho.widget.SimpleStateUpdateEmulator;
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class MountStateIncrementalMountTest {

  private final boolean mExtensionAcquireDuringMount;
  private ComponentContext mContext;
  final boolean mUseMountDelegateTarget;
  final boolean mDelegateToRenderCoreMount;
  private ShadowLooper mLayoutThreadShadowLooper;

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  private boolean mExtensionAcquireDuringMountDefault;

  @ParameterizedRobolectricTestRunner.Parameters(
      name =
          "useMountDelegateTarget={0}, delegateToRenderCoreMount={1}, extensionAcquireDuringMount={3}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false, false, false},
          {true, false, false},
          {true, true, false},
          {false, false, false},
          {true, false, true},
          {true, true, true},
          {false, false, true},
        });
  }

  public MountStateIncrementalMountTest(
      boolean useMountDelegateTarget,
      boolean delegateToRenderCoreMount,
      boolean extensionAcquireDuringMount) {
    mUseMountDelegateTarget = useMountDelegateTarget;
    mDelegateToRenderCoreMount = delegateToRenderCoreMount;
    mExtensionAcquireDuringMount = extensionAcquireDuringMount;
  }

  @Before
  public void setup() {
    mExtensionAcquireDuringMountDefault = ComponentsConfiguration.extensionAcquireDuringMount;
    ComponentsConfiguration.extensionAcquireDuringMount = mExtensionAcquireDuringMount;
    mContext = mLithoViewRule.getContext();
    mLithoViewRule.useLithoView(
        new LithoView(mContext, mUseMountDelegateTarget, mDelegateToRenderCoreMount));
    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
  }

  @After
  public void cleanup() {
    ComponentsConfiguration.extensionAcquireDuringMount = mExtensionAcquireDuringMountDefault;
  }

  /** Tests incremental mount behaviour of a vertical stack of components with a View mount type. */
  @Test
  public void testIncrementalMountVerticalViewStackScrollUp() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 15, 10, 25), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 20, 10, 30), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
  }

  @Test
  public void testIncrementalMountVerticalViewStackScrollDown() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 20, 10, 30), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 15, 10, 25), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 9), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
  }

  @Test
  public void incrementalMount_visibleTopIntersectsItemBottom_unmountItem() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final TestComponent child3 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child3).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 10, 10, 30), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();
    assertThat(child3.isMounted()).isTrue();
  }

  @Test
  public void incrementalMount_visibleBottomIntersectsItemTop_unmountItem() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final TestComponent child3 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child3).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 20), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
    assertThat(child3.isMounted()).isFalse();
  }

  @Test
  public void incrementalMount_visibleRectIntersectsItemBounds_mountItem() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final TestComponent child3 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child3).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 10, 10, 20), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();
    assertThat(child3.isMounted()).isFalse();
  }

  @Test
  public void incrementalMount_visibleBoundsEmpty_unmountAllItems() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final TestComponent child3 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child3).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 0, 0), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    assertThat(child3.isMounted()).isFalse();
  }

  @Test
  public void incrementalMount_emptyItemBoundsIntersectVisibleRect_mountItem() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final TestComponent child3 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(0))
            .child(Wrapper.create(mContext).delegate(child3).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 30), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
    assertThat(child3.isMounted()).isTrue();
  }

  @Test
  public void incrementalMount_emptyItemBoundsEmptyVisibleRect_unmountItem() {
    final TestComponent child1 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(0))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 0), true);
    assertThat(child1.isMounted()).isFalse();
  }

  /**
   * Tests incremental mount behaviour of a horizontal stack of components with a View mount type.
   */
  @Test
  public void testIncrementalMountHorizontalViewStack() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final Component root =
        Row.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(-10, 0, -5, 10), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 5, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(5, 0, 15, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(15, 0, 25, 10), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(20, 0, 30, 10), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a Drawable mount type.
   */
  @Test
  public void testIncrementalMountVerticalDrawableStack() {
    final LifecycleTracker lifecycleTracker1 = new LifecycleTracker();
    final Component child1 =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(lifecycleTracker1).build();
    final LifecycleTracker lifecycleTracker2 = new LifecycleTracker();
    final Component child2 =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(lifecycleTracker2).build();

    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(lifecycleTracker1.isMounted()).isFalse();
    assertThat(lifecycleTracker2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(lifecycleTracker1.isMounted()).isTrue();
    assertThat(lifecycleTracker2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(lifecycleTracker1.isMounted()).isTrue();
    assertThat(lifecycleTracker2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 15, 10, 25), true);
    assertThat(lifecycleTracker1.isMounted()).isFalse();
    assertThat(lifecycleTracker2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 20, 10, 30), true);
    assertThat(lifecycleTracker1.isMounted()).isFalse();
    assertThat(lifecycleTracker2.isMounted()).isFalse();
  }

  /** Tests incremental mount behaviour of a view mount item in a nested hierarchy. */
  @Test
  public void testIncrementalMountNestedView() {
    final TestComponent child = create(mContext).build();

    final Component root =
        Column.create(mContext)
            .wrapInView()
            .paddingPx(ALL, 20)
            .child(Wrapper.create(mContext).delegate(child).widthPx(10).heightPx(10))
            .child(SimpleMountSpecTester.create(mContext))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 50, 20), true);
    assertThat(child.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 50, 40), true);
    assertThat(child.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(30, 0, 50, 40), true);
    assertThat(child.isMounted()).isFalse();
  }

  /**
   * Verify that we can cope with a negative padding on a component that is wrapped in a view (since
   * the bounds of the component will be larger than the bounds of the view).
   */
  @Test
  public void testIncrementalMountVerticalDrawableStackNegativeMargin() {
    final FrameLayout parent = new FrameLayout(mContext.getAndroidContext());
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
    parent.layout(0, 0, 10, 1000);

    mLithoViewRule
        .setRoot(Row.create(mContext).build())
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();
    parent.addView(lithoView);

    lithoView.setTranslationY(105);

    final EventHandler eventHandler = mock(EventHandler.class);
    final LifecycleTracker lifecycleTracker1 = new LifecycleTracker();
    final Component child1 =
        MountSpecLifecycleTesterDrawable.create(mContext)
            .lifecycleTracker(lifecycleTracker1)
            .build();
    final Component childHost1 =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(child1)
                    .widthPx(10)
                    .heightPx(10)
                    .clickHandler(eventHandler)
                    .marginDip(YogaEdge.TOP, -10))
            .build();

    final Component rootHost =
        Row.create(mContext)
            .child(Wrapper.create(mContext).delegate(childHost1).clickHandler(eventHandler).build())
            .build();

    lithoView.getComponentTree().setRoot(rootHost);

    assertThat(lifecycleTracker1.getSteps()).contains(LifecycleStep.ON_MOUNT);
  }

  @Test
  public void testIncrementalMountVerticalDrawableStackNegativeMargin_multipleUnmountedHosts() {
    final FrameLayout parent = new FrameLayout(mContext.getAndroidContext());
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
    parent.layout(0, 0, 10, 1000);

    mLithoViewRule
        .setRoot(Row.create(mContext).build())
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();
    parent.addView(lithoView);

    lithoView.setTranslationY(105);

    final EventHandler eventHandler = mock(EventHandler.class);
    final LifecycleTracker lifecycleTracker1 = new LifecycleTracker();
    final Component child1 =
        MountSpecLifecycleTesterDrawable.create(mContext)
            .lifecycleTracker(lifecycleTracker1)
            .build();
    final Component childHost1 =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(child1)
                    .widthPx(10)
                    .heightPx(10)
                    .clickHandler(eventHandler)
                    .marginDip(YogaEdge.TOP, -10))
            .build();

    final Component rootHost =
        Row.create(mContext)
            .child(
                Row.create(mContext)
                    .viewTag("extra_host")
                    .child(
                        Wrapper.create(mContext)
                            .delegate(childHost1)
                            .clickHandler(eventHandler)
                            .build())
                    .child(
                        Wrapper.create(mContext)
                            .delegate(childHost1)
                            .clickHandler(eventHandler)
                            .build()))
            .build();

    lithoView.getComponentTree().setRoot(rootHost);

    assertThat(lifecycleTracker1.getSteps()).contains(LifecycleStep.ON_MOUNT);
  }

  @Test
  public void itemWithNegativeMargin_removeAndAdd_hostIsMounted() {
    final FrameLayout parent = new FrameLayout(mContext.getAndroidContext());
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
    parent.layout(0, 0, 10, 1000);

    mLithoViewRule
        .setRoot(Row.create(mContext).build())
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();
    parent.addView(lithoView);

    lithoView.setTranslationY(95);

    final EventHandler eventHandler1 = mock(EventHandler.class);
    final LifecycleTracker lifecycleTracker1 = new LifecycleTracker();
    final Component child1 =
        MountSpecLifecycleTesterDrawable.create(mContext)
            .lifecycleTracker(lifecycleTracker1)
            .build();
    final Component childHost1 =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(child1)
                    .widthPx(10)
                    .heightPx(10)
                    .clickHandler(eventHandler1))
            .build();

    final Component host1 =
        Row.create(mContext)
            .child(
                Wrapper.create(mContext).delegate(childHost1).clickHandler(eventHandler1).build())
            .build();

    final EventHandler eventHandler2 = mock(EventHandler.class);
    final LifecycleTracker lifecycleTracker2 = new LifecycleTracker();
    final Component child2 =
        MountSpecLifecycleTesterDrawable.create(mContext)
            .lifecycleTracker(lifecycleTracker2)
            .build();
    final Component childHost2 =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(child2)
                    .widthPx(10)
                    .heightPx(10)
                    .clickHandler(eventHandler2)
                    .marginDip(YogaEdge.TOP, -10))
            .build();

    final Component host2 =
        Row.create(mContext)
            .child(
                Wrapper.create(mContext).delegate(childHost2).clickHandler(eventHandler2).build())
            .build();

    final Component rootHost = Column.create(mContext).child(host1).child(host2).build();

    // Mount both child1 and child2.
    lithoView.getComponentTree().setRoot(rootHost);

    assertThat(lifecycleTracker2.getSteps()).contains(LifecycleStep.ON_MOUNT);
    lifecycleTracker2.reset();

    // Remove child2.
    final Component newHost = Column.create(mContext).child(host1).build();
    lithoView.getComponentTree().setRoot(newHost);

    // Add child2 back.
    assertThat(lifecycleTracker2.getSteps()).contains(LifecycleStep.ON_UNMOUNT);
    lifecycleTracker2.reset();

    lithoView.getComponentTree().setRoot(rootHost);

    assertThat(lifecycleTracker2.getSteps()).contains(LifecycleStep.ON_MOUNT);
  }

  /** Tests incremental mount behaviour of overlapping view mount items. */
  @Test
  public void testIncrementalMountOverlappingView() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(child1)
                    .positionType(ABSOLUTE)
                    .positionPx(TOP, 0)
                    .positionPx(LEFT, 0)
                    .widthPx(10)
                    .heightPx(10))
            .child(
                Wrapper.create(mContext)
                    .delegate(child2)
                    .positionType(ABSOLUTE)
                    .positionPx(TOP, 5)
                    .positionPx(LEFT, 5)
                    .widthPx(10)
                    .heightPx(10))
            .child(SimpleMountSpecTester.create(mContext))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 5, 5), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(5, 5, 10, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(10, 10, 15, 15), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(15, 15, 20, 20), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
  }

  @Test
  public void testChildViewGroupIncrementallyMounted() {
    final ViewGroup mountedView = mock(ViewGroup.class);
    when(mountedView.getChildCount()).thenReturn(3);

    final LithoView childView1 = getMockLithoViewWithBounds(new Rect(5, 10, 20, 30));
    when(mountedView.getChildAt(0)).thenReturn(childView1);

    final LithoView childView2 = getMockLithoViewWithBounds(new Rect(10, 10, 50, 60));
    when(mountedView.getChildAt(1)).thenReturn(childView2);

    final LithoView childView3 = getMockLithoViewWithBounds(new Rect(30, 35, 50, 60));
    when(mountedView.getChildAt(2)).thenReturn(childView3);

    final Component root = TestViewComponent.create(mContext).testView(mountedView).build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    verify(childView1).notifyVisibleBoundsChanged();
    verify(childView2).notifyVisibleBoundsChanged();
    verify(childView3).notifyVisibleBoundsChanged();

    reset(childView1);
    reset(childView2);
    reset(childView3);

    lithoView.getComponentTree().mountComponent(new Rect(15, 15, 40, 40), true);

    // Called twice when mount is delegated; for both incremental mount and visibility extension
    verify(childView1, times(mUseMountDelegateTarget || mDelegateToRenderCoreMount ? 2 : 1))
        .notifyVisibleBoundsChanged();
    verify(childView2, times(mUseMountDelegateTarget || mDelegateToRenderCoreMount ? 2 : 1))
        .notifyVisibleBoundsChanged();
    verify(childView3, times(mUseMountDelegateTarget || mDelegateToRenderCoreMount ? 2 : 1))
        .notifyVisibleBoundsChanged();
  }

  @Test
  public void testChildViewGroupAllIncrementallyMountedNotProcessVisibilityOutputs() {
    final ViewGroup mountedView = mock(ViewGroup.class);
    when(mountedView.getLeft()).thenReturn(0);
    when(mountedView.getTop()).thenReturn(0);
    when(mountedView.getRight()).thenReturn(100);
    when(mountedView.getBottom()).thenReturn(100);
    when(mountedView.getChildCount()).thenReturn(3);

    final LithoView childView1 = getMockLithoViewWithBounds(new Rect(5, 10, 20, 30));
    when(childView1.getTranslationX()).thenReturn(5.0f);
    when(childView1.getTranslationY()).thenReturn(-10.0f);
    when(mountedView.getChildAt(0)).thenReturn(childView1);

    final LithoView childView2 = getMockLithoViewWithBounds(new Rect(10, 10, 50, 60));
    when(mountedView.getChildAt(1)).thenReturn(childView2);

    final LithoView childView3 = getMockLithoViewWithBounds(new Rect(30, 35, 50, 60));
    when(mountedView.getChildAt(2)).thenReturn(childView3);

    final Component root = TestViewComponent.create(mContext).testView(mountedView).build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    // Can't verify directly as the object will have changed by the time we get the chance to
    // verify it.
    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                if (!rect.equals(new Rect(0, 0, 15, 20))) {
                  fail();
                }
                return null;
              }
            })
        .when(childView1)
        .notifyVisibleBoundsChanged((Rect) any(), eq(true));

    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                if (!rect.equals(new Rect(0, 0, 40, 50))) {
                  fail();
                }
                return null;
              }
            })
        .when(childView2)
        .notifyVisibleBoundsChanged((Rect) any(), eq(true));

    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                if (!rect.equals(new Rect(0, 0, 20, 25))) {
                  fail();
                }
                return null;
              }
            })
        .when(childView3)
        .notifyVisibleBoundsChanged((Rect) any(), eq(true));

    verify(childView1).notifyVisibleBoundsChanged();
    verify(childView2).notifyVisibleBoundsChanged();
    verify(childView3).notifyVisibleBoundsChanged();

    reset(childView1);
    reset(childView2);
    reset(childView3);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 100, 100), true);

    // Called twice when mount is delegated; for both incremental mount and visibility extension
    verify(childView1, times(mUseMountDelegateTarget || mDelegateToRenderCoreMount ? 2 : 1))
        .notifyVisibleBoundsChanged();
    verify(childView2, times(mUseMountDelegateTarget || mDelegateToRenderCoreMount ? 2 : 1))
        .notifyVisibleBoundsChanged();
    verify(childView3, times(mUseMountDelegateTarget || mDelegateToRenderCoreMount ? 2 : 1))
        .notifyVisibleBoundsChanged();
  }

  /** Tests incremental mount behaviour of a vertical stack of components with a View mount type. */
  @Test
  public void testIncrementalMountDoesNotCauseMultipleUpdates() {
    final TestComponent child1 = create(mContext).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child1.wasOnUnbindCalled()).isTrue();
    assertThat(child1.wasOnUnmountCalled()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(child1.isMounted()).isTrue();

    child1.resetInteractions();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();

    assertThat(child1.wasOnBindCalled()).isFalse();
    assertThat(child1.wasOnMountCalled()).isFalse();
    assertThat(child1.wasOnUnbindCalled()).isFalse();
    assertThat(child1.wasOnUnmountCalled()).isFalse();
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a Drawable mount type
   * after unmountAllItems was called.
   */
  @Test
  public void testIncrementalMountAfterUnmountAllItemsCall() {
    final LifecycleTracker lifecycleTracker1 = new LifecycleTracker();
    final LifecycleTracker lifecycleTracker2 = new LifecycleTracker();
    final Component child1 =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(lifecycleTracker1).build();
    final Component child2 =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(lifecycleTracker2).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(lifecycleTracker1.isMounted()).isFalse();
    assertThat(lifecycleTracker2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(lifecycleTracker1.isMounted()).isTrue();
    assertThat(lifecycleTracker2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(lifecycleTracker1.isMounted()).isTrue();
    assertThat(lifecycleTracker2.isMounted()).isTrue();

    lithoView.unmountAllItems();
    assertThat(lifecycleTracker1.isMounted()).isFalse();
    assertThat(lifecycleTracker2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(lifecycleTracker1.isMounted()).isTrue();
    assertThat(lifecycleTracker2.isMounted()).isTrue();
  }

  @Test
  public void testMountStateNeedsRemount_incrementalMountAfterUnmount_isFalse() {
    final LifecycleTracker lifecycleTracker1 = new LifecycleTracker();
    final LifecycleTracker lifecycleTracker2 = new LifecycleTracker();
    final Component child1 =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(lifecycleTracker1).build();
    final Component child2 =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(lifecycleTracker2).build();
    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();
    assertThat(lithoView.mountStateNeedsRemount()).isFalse();

    lithoView.unmountAllItems();
    assertThat(lithoView.mountStateNeedsRemount()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(lithoView.mountStateNeedsRemount()).isFalse();
  }

  @Test
  public void testRootViewAttributes_incrementalMountAfterUnmount_setViewAttributes() {
    final Component root = Text.create(mContext).text("Test").contentDescription("testcd").build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();
    assertThat(lithoView.getContentDescription()).isEqualTo("testcd");

    lithoView.unmountAllItems();
    assertThat(lithoView.getContentDescription()).isNull();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(lithoView.getContentDescription()).isEqualTo("testcd");
  }

  /**
   * Tests incremental mount behaviour of a nested Litho View. We want to ensure that when a child
   * view is first mounted due to a layout pass it does not also have notifyVisibleBoundsChanged
   * called on it.
   */
  @Test
  public void testIncrementalMountAfterLithoViewIsMounted() {
    final LithoView lithoView = mock(LithoView.class);
    when(lithoView.isIncrementalMountEnabled()).thenReturn(true);

    final ViewGroupWithLithoViewChildren viewGroup =
        new ViewGroupWithLithoViewChildren(mContext.getAndroidContext());
    viewGroup.addView(lithoView);

    final Component root =
        TestViewComponent.create(mContext, true, true, true, true).testView(viewGroup).build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoViewParent = mLithoViewRule.getLithoView();

    verify(lithoView).notifyVisibleBoundsChanged();
    reset(lithoView);

    // Mount views with visible rect
    lithoViewParent.getComponentTree().mountComponent(new Rect(0, 0, 100, 1000), true);
    verify(lithoView, times(mUseMountDelegateTarget || mDelegateToRenderCoreMount ? 2 : 1))
        .notifyVisibleBoundsChanged();
    reset(lithoView);
    when(lithoView.isIncrementalMountEnabled()).thenReturn(true);

    // Unmount views with visible rect outside
    lithoViewParent.getComponentTree().mountComponent(new Rect(0, -10, 100, -5), true);
    verify(lithoView, never()).notifyVisibleBoundsChanged();
    reset(lithoView);
    when(lithoView.isIncrementalMountEnabled()).thenReturn(true);

    // Mount again with visible rect
    lithoViewParent.getComponentTree().mountComponent(new Rect(0, 0, 100, 1000), true);

    verify(lithoView, times(1)).notifyVisibleBoundsChanged();
  }

  @Test
  public void incrementalMount_dirtyMount_unmountItemsOffScreen() {
    final LifecycleTracker info_child1 = new LifecycleTracker();
    final LifecycleTracker info_child2 = new LifecycleTracker();
    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater =
        new SimpleStateUpdateEmulatorSpec.Caller();

    final Component root =
        Column.create(mLithoViewRule.getContext())
            .child(
                MountSpecLifecycleTester.create(mLithoViewRule.getContext())
                    .intrinsicSize(new Size(10, 10))
                    .lifecycleTracker(info_child1)
                    .key("some_key"))
            .child(
                MountSpecLifecycleTester.create(mLithoViewRule.getContext())
                    .intrinsicSize(new Size(10, 10))
                    .lifecycleTracker(info_child2)
                    .key("other_key"))
            .child(
                SimpleStateUpdateEmulator.create(mLithoViewRule.getContext()).caller(stateUpdater))
            .build();

    mLithoViewRule.setRoot(root).setSizePx(10, 20).attachToWindow().measure().layout();

    final FrameLayout parent = new FrameLayout(mContext.getAndroidContext());
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    parent.layout(0, 0, 10, 20);

    parent.addView(mLithoViewRule.getLithoView(), 0, 20);

    assertThat(info_child1.getSteps()).describedAs("Mounted.").contains(ON_MOUNT);
    assertThat(info_child2.getSteps()).describedAs("Mounted.").contains(ON_MOUNT);

    stateUpdater.increment();

    info_child1.reset();
    info_child2.reset();

    mLithoViewRule.getLithoView().setTranslationY(-12);
    assertThat(info_child1.getSteps()).describedAs("Mounted.").contains(ON_UNMOUNT);
  }

  @Test
  public void incrementalMount_setVisibilityHintFalse_preventMount() {
    ComponentsConfiguration.skipIncrementalMountOnSetVisibilityHintFalse = true;
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();

    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(child1, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(child1, 2);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(child1)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .child(
                Wrapper.create(mContext)
                    .delegate(child2)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(20, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);

    assertThat(child2.isMounted()).isFalse();

    child1.getDispatchedEventHandlers().clear();
    child1.resetInteractions();

    lithoView.setVisibilityHint(false);

    assertThat(child1.wasOnMountCalled()).isFalse();
    assertThat(child1.wasOnUnmountCalled()).isFalse();
    assertThat(child1.getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(child1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    child1.getDispatchedEventHandlers().clear();
    child1.resetInteractions();
    child2.resetInteractions();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 20), true);

    assertThat(child2.wasOnMountCalled()).isFalse();
    assertThat(child1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);
    assertThat(child1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler);
    ComponentsConfiguration.skipIncrementalMountOnSetVisibilityHintFalse = false;
  }

  @Test
  public void incrementalMount_setVisibilityHintTrue_mountIfNeeded() {
    ComponentsConfiguration.skipIncrementalMountOnSetVisibilityHintFalse = true;
    final TestComponent child1 = create(mContext).build();

    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(child1, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler1 = new EventHandler<>(child1, 2);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(child1)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLithoViewRule.getLithoView();

    assertThat(child1.getDispatchedEventHandlers()).contains(visibleEventHandler1);

    lithoView.setVisibilityHint(false);

    final TestComponent child2 = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(child2, 3);
    final EventHandler<InvisibleEvent> invisibleEventHandler2 = new EventHandler<>(child2, 4);
    final Component newRoot =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(child1)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .widthPx(10)
                    .heightPx(10))
            .child(
                Wrapper.create(mContext)
                    .delegate(child2)
                    .visibleHandler(visibleEventHandler2)
                    .invisibleHandler(invisibleEventHandler2)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    lithoView.getComponentTree().setRoot(newRoot);
    assertThat(child2.wasOnMountCalled()).isFalse();
    assertThat(child2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);

    lithoView.setVisibilityHint(true);
    assertThat(child2.wasOnMountCalled()).isTrue();
    assertThat(child2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    ComponentsConfiguration.skipIncrementalMountOnSetVisibilityHintFalse = false;
  }

  @Test
  public void dirtyMount_visibleRectChanged_unmountItemNotInVisibleBounds() {
    final LifecycleTracker lifecycleTracker1 = new LifecycleTracker();
    final LifecycleTracker lifecycleTracker2 = new LifecycleTracker();
    final LifecycleTracker lifecycleTracker3 = new LifecycleTracker();

    final Component child1 =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(lifecycleTracker1).build();
    final Component child2 =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(lifecycleTracker2).build();
    final Component child3 =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(lifecycleTracker3).build();

    final Component root1 =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child3).widthPx(10).heightPx(10))
            .build();

    final RecyclerBinderConfiguration binderConfig =
        RecyclerBinderConfiguration.create().lithoViewFactory(getLithoViewFactory()).build();
    RecyclerConfiguration config =
        ListRecyclerConfiguration.create().recyclerBinderConfiguration(binderConfig).build();

    final Component rcc =
        RecyclerCollectionComponent.create(mContext)
            .recyclerConfiguration(config)
            .section(
                SingleComponentSection.create(new SectionContext(mContext))
                    .component(root1)
                    .build())
            .build();

    mLithoViewRule
        .setRoot(rcc)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(19, EXACTLY))
        .measure()
        .layout();

    assertThat(lifecycleTracker1.getSteps()).contains(LifecycleStep.ON_MOUNT);
    assertThat(lifecycleTracker2.getSteps()).contains(LifecycleStep.ON_MOUNT);
    assertThat(lifecycleTracker3.getSteps()).doesNotContain(LifecycleStep.ON_MOUNT);

    lifecycleTracker1.reset();
    lifecycleTracker2.reset();
    lifecycleTracker3.reset();

    final Component root2 =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(
                        MountSpecLifecycleTester.create(mContext)
                            .lifecycleTracker(lifecycleTracker1)
                            .build())
                    .widthPx(10)
                    .heightPx(20))
            .child(Wrapper.create(mContext).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(mContext).delegate(child3).widthPx(10).heightPx(10))
            .build();

    final Component rcc2 =
        RecyclerCollectionComponent.create(mContext)
            .recyclerConfiguration(config)
            .section(
                SingleComponentSection.create(new SectionContext(mContext))
                    .component(root2)
                    .build())
            .build();

    mLithoViewRule.setRoot(rcc2);

    mLayoutThreadShadowLooper.runToEndOfTasks();
    mLithoViewRule.getLithoView().notifyVisibleBoundsChanged();

    assertThat(lifecycleTracker2.getSteps()).contains(LifecycleStep.ON_UNMOUNT);
  }

  private LithoViewFactory getLithoViewFactory() {
    return new LithoViewFactory() {
      @Override
      public LithoView createLithoView(ComponentContext context) {
        return new LithoView(context, mUseMountDelegateTarget, mDelegateToRenderCoreMount);
      }
    };
  }

  private static LithoView getMockLithoViewWithBounds(Rect bounds) {
    final LithoView lithoView = mock(LithoView.class);
    when(lithoView.getLeft()).thenReturn(bounds.left);
    when(lithoView.getTop()).thenReturn(bounds.top);
    when(lithoView.getRight()).thenReturn(bounds.right);
    when(lithoView.getBottom()).thenReturn(bounds.bottom);
    when(lithoView.getWidth()).thenReturn(bounds.width());
    when(lithoView.getHeight()).thenReturn(bounds.height());
    when(lithoView.isIncrementalMountEnabled()).thenReturn(true);

    return lithoView;
  }

  private static class TestLithoView extends LithoView {
    private final Rect mPreviousIncrementalMountBounds = new Rect();

    public TestLithoView(Context context) {
      super(context);
    }

    @Override
    public void notifyVisibleBoundsChanged(Rect visibleRect, boolean processVisibilityOutputs) {
      System.out.println("performIncMount on TestLithoView");
      mPreviousIncrementalMountBounds.set(visibleRect);
    }

    private Rect getPreviousIncrementalMountBounds() {
      return mPreviousIncrementalMountBounds;
    }

    @Override
    public boolean isIncrementalMountEnabled() {
      return true;
    }
  }
}
