/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.os.Looper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

/** Tests for {@link ComponentTreeHolder} */
@RunWith(ComponentsTestRunner.class)
public class ComponentTreeHolderTest {

  private ComponentContext mContext;
  private Component mComponent;
  private ComponentRenderInfo mComponentRenderInfo;
  private ViewRenderInfo mViewRenderInfo;
  private ShadowLooper mLayoutThreadShadowLooper;
  private int mWidthSpec = SizeSpec.makeSizeSpec(100, EXACTLY);
  private int mHeightSpec = SizeSpec.makeSizeSpec(100, EXACTLY);
  private int mWidthSpec2 = SizeSpec.makeSizeSpec(101, EXACTLY);
  private int mHeightSpec2 = SizeSpec.makeSizeSpec(101, EXACTLY);

  @Before
  public void setUp() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponent = TestDrawableComponent.create(mContext).build();
    mComponentRenderInfo = ComponentRenderInfo.create().component(mComponent).build();
    mViewRenderInfo =
        ViewRenderInfo.create()
            .customViewType(0)
            .viewBinder(mock(ViewBinder.class))
            .viewCreator(mock(ViewCreator.class))
            .build();

    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
  }

  @Test
  public void testHasCompletedLatestLayoutWhenNoLayoutComputed() {
    ComponentTreeHolder holder = createComponentTreeHolder(mComponentRenderInfo);
    assertThat(holder.hasCompletedLatestLayout()).isFalse();
  }

  @Test
  public void testHasCompletedLatestLayoutForSyncRender() {
    ComponentTreeHolder holder = createComponentTreeHolder(mComponentRenderInfo);
    holder.computeLayoutSync(mContext, mWidthSpec, mHeightSpec, new Size());

    assertThat(holder.hasCompletedLatestLayout()).isTrue();
  }

  @Test
  public void testHasCompletedLatestLayoutForAsyncRender() {
    ComponentTreeHolder holder = createComponentTreeHolder(mComponentRenderInfo);
    holder.computeLayoutAsync(mContext, mWidthSpec, mHeightSpec);

    assertThat(holder.hasCompletedLatestLayout()).isFalse();

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(holder.hasCompletedLatestLayout()).isTrue();

    // Re-computing with the same async layout specs shouldn't invalidate the layout
    holder.computeLayoutAsync(mContext, mWidthSpec, mHeightSpec);

    assertThat(holder.hasCompletedLatestLayout()).isTrue();
  }

  @Test
  public void testHasCompletedLatestLayoutForAsyncRenderAfterSyncRender() {
    ComponentTreeHolder holder = createComponentTreeHolder(mComponentRenderInfo);
    holder.computeLayoutSync(mContext, mWidthSpec, mHeightSpec, new Size());

    assertThat(holder.hasCompletedLatestLayout()).isTrue();

    holder.computeLayoutAsync(mContext, mWidthSpec2, mHeightSpec2);

    assertThat(holder.hasCompletedLatestLayout()).isFalse();
  }

  @Test
  public void testHasCompletedLatestLayoutForViewBasedTreeHolder() {
    ComponentTreeHolder holder = createComponentTreeHolder(mViewRenderInfo);
    assertThat(holder.hasCompletedLatestLayout()).isTrue();
  }

  @Test
  public void testSetListenerBeforeTreeCreation() {
    ComponentTreeHolder holder = createComponentTreeHolder(mComponentRenderInfo);
    ComponentTree.NewLayoutStateReadyListener listener =
        mock(ComponentTree.NewLayoutStateReadyListener.class);
    holder.setNewLayoutReadyListener(listener);

    assertThat(holder.getComponentTree()).isNull();

    holder.computeLayoutSync(
        mContext,
        SizeSpec.makeSizeSpec(100, EXACTLY),
        SizeSpec.makeSizeSpec(100, EXACTLY),
        new Size());

    assertThat(holder.getComponentTree().getNewLayoutStateReadyListener()).isEqualTo(listener);

    holder.setNewLayoutReadyListener(null);

    assertThat(holder.getComponentTree().getNewLayoutStateReadyListener()).isNull();
  }

  @Test
  public void testSetListenerAfterTreeCreation() {
    ComponentTreeHolder holder = createComponentTreeHolder(mComponentRenderInfo);
    ComponentTree.NewLayoutStateReadyListener listener =
        mock(ComponentTree.NewLayoutStateReadyListener.class);

    holder.computeLayoutSync(
        mContext,
        SizeSpec.makeSizeSpec(100, EXACTLY),
        SizeSpec.makeSizeSpec(100, EXACTLY),
        new Size());

    holder.setNewLayoutReadyListener(listener);
    assertThat(holder.getComponentTree().getNewLayoutStateReadyListener()).isEqualTo(listener);
  }

  @Test
  public void testSetListenerOnViewRenderInfoDoesNotCrash() {
    ComponentTreeHolder holder = createComponentTreeHolder(mViewRenderInfo);
    ComponentTree.NewLayoutStateReadyListener listener =
        mock(ComponentTree.NewLayoutStateReadyListener.class);

    holder.setNewLayoutReadyListener(listener);
  }

  private ComponentTreeHolder createComponentTreeHolder(RenderInfo info) {
    return ComponentTreeHolder.acquire(info, null, false, false);
  }
}
