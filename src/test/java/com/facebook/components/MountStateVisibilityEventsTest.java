/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;
import android.support.v4.util.LongSparseArray;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestLayoutComponent;
import com.facebook.components.testing.TestViewComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(ComponentsTestRunner.class)
public class MountStateVisibilityEventsTest {

  private static final int VISIBLE = 1;
  private static final int FOCUSED = 2;
  private static final int FULL_IMPRESSION = 3;
  private static final int INVISIBLE = 4;

  private static final int LEFT = 0;
  private static final int RIGHT = 10;

  private static final int VIEWPORT_HEIGHT = 5;
  private static final int VIEWPORT_WIDTH = 10;

  private long mLastVisibilityOutputId = 0;
  private ComponentContext mContext;
  private MountState mMountState;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);

    ComponentTree mockComponentTree = mock(ComponentTree.class);
    doReturn(mContext).when(mockComponentTree).getContext();

    ComponentHost mockParent = mock(ComponentHost.class);
    doReturn(VIEWPORT_WIDTH).when(mockParent).getWidth();
    doReturn(VIEWPORT_HEIGHT).when(mockParent).getHeight();

    ComponentView attachedView = spy(new ComponentView(mContext));
    Whitebox.setInternalState(attachedView, "mComponent", mockComponentTree);
    doReturn(mockParent).when(attachedView).getParent();

    mMountState = new MountState(attachedView);
    Whitebox.setInternalState(mMountState, "mIsDirty", false);

    mLastVisibilityOutputId = 0;
  }

  @Test
  public void testVisibleEvent() {
    ComponentLifecycle mockLifecycle = createLifecycleMock();
    Component<?> content = TestViewComponent.create(mContext).build();
    Whitebox.setInternalState(content, "mLifecycle", mockLifecycle);

    final EventHandler visibleHandler = createEventHandler(content, VISIBLE);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();

    visibilityOutputs.add(createVisibilityOutput(
        content,
        new Rect(LEFT, 5, RIGHT, 10),
        visibleHandler,
        null,
        null,
        null));

    final LayoutState layoutState = new LayoutState();
    Whitebox.setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    mMountState.mount(layoutState, new Rect(LEFT, 0, RIGHT, 5));
