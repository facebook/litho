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

package com.facebook.litho.widget;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.os.Looper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.RenderCompleteEvent;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

/** Tests for {@link ComponentTreeHolder} */
@RunWith(ComponentsTestRunner.class)
public class ComponentTreeHolderTest {

  private ComponentContext mContext;
  private Component mComponent;
  private EventHandler<RenderCompleteEvent> mRenderCompleteEventHandler;
  private ComponentRenderInfo mComponentRenderInfo;
  private ViewRenderInfo mViewRenderInfo;
  private ShadowLooper mLayoutThreadShadowLooper;
  private int mWidthSpec = SizeSpec.makeSizeSpec(100, EXACTLY);
  private int mHeightSpec = SizeSpec.makeSizeSpec(100, EXACTLY);
  private int mWidthSpec2 = SizeSpec.makeSizeSpec(101, EXACTLY);
  private int mHeightSpec2 = SizeSpec.makeSizeSpec(101, EXACTLY);

  @Before
  public void setUp() throws Exception {
    mContext = new ComponentContext(getApplicationContext());
    mComponent = TestDrawableComponent.create(mContext).build();
    mRenderCompleteEventHandler = (EventHandler<RenderCompleteEvent>) mock(EventHandler.class);
    mComponentRenderInfo =
        ComponentRenderInfo.create()
            .component(mComponent)
            .renderCompleteHandler(mRenderCompleteEventHandler)
            .build();
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
  public void testRetainAnimationStateAfterExitingRange() {
    ComponentTreeHolder holder = createComponentTreeHolder(mComponentRenderInfo);
    holder.computeLayoutSync(mContext, mWidthSpec, mHeightSpec, new Size());
    assertThat(holder.getComponentTree().hasMounted()).isFalse();
    Whitebox.setInternalState(holder.getComponentTree(), "mHasMounted", true);

    // component goes out of range
    holder.acquireStateAndReleaseTree();
    assertThat(holder.getComponentTree()).isNull();

    // component comes back within range
    holder.computeLayoutSync(mContext, mWidthSpec, mHeightSpec, new Size());
    assertThat(holder.getComponentTree().hasMounted()).isTrue();
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

  @Test
  public void testGetRenderCompleteHandlerDoesNotCrash() {
    ComponentTreeHolder holder = createComponentTreeHolder(mComponentRenderInfo);
    holder.getRenderInfo().getRenderCompleteEventHandler();
  }

  private ComponentTreeHolder createComponentTreeHolder(RenderInfo info) {
    return ComponentTreeHolder.create().renderInfo(info).build();
  }
}
