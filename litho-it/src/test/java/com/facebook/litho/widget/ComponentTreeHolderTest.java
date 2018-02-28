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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LayoutHandler;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link ComponentTreeHolder} */
@RunWith(ComponentsTestRunner.class)
public class ComponentTreeHolderTest {

  private ComponentContext mContext;
  private Component mComponent;
  private ComponentRenderInfo mComponentRenderInfo;
  private ViewRenderInfo mViewRenderInfo;

  @Before
  public void setUp() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponent = TestDrawableComponent.create(mContext).build();
    mComponentRenderInfo = ComponentRenderInfo.create().component(mComponent).build();
    mViewRenderInfo =
        ViewRenderInfo.create()
            .customViewType(0)
            .viewBinder(mock(ViewBinder.class))
            .viewCreator(mock(ViewCreator.class))
            .build();
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
    return ComponentTreeHolder.acquire(info, mock(LayoutHandler.class), false, false);
  }
}
