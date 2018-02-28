/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.ComponentTree.create;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import android.os.Looper;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeNewLayoutStateReadyListenerTest {

  private int mWidthSpec;
  private int mWidthSpec2;
  private int mHeightSpec;
  private int mHeightSpec2;

  private Component mComponent;
  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private ComponentTree mComponentTree;
  private ComponentTree.NewLayoutStateReadyListener mListener;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponent = TestDrawableComponent.create(mContext).build();
    mComponentTree = create(mContext, mComponent).build();

    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));

    mWidthSpec = makeSizeSpec(39, EXACTLY);
    mWidthSpec2 = makeSizeSpec(40, EXACTLY);
    mHeightSpec = makeSizeSpec(41, EXACTLY);
    mHeightSpec2 = makeSizeSpec(42, EXACTLY);

    mListener = mock(ComponentTree.NewLayoutStateReadyListener.class);
  }

  @Test
  public void testListenerInvokedForSetRoot() {
    mComponentTree.setNewLayoutStateReadyListener(mListener);
    mComponentTree.setRootAndSizeSpec(mComponent, mWidthSpec, mHeightSpec);

    verify(mListener).onNewLayoutStateReady(mComponentTree);
  }

  @Test
  public void testListenerInvokedForSetRootAsync() {
    mComponentTree.setNewLayoutStateReadyListener(mListener);
    mComponentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);

    verify(mListener, never()).onNewLayoutStateReady(any(ComponentTree.class));

    // Now the background thread run the queued task.
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(mListener).onNewLayoutStateReady(mComponentTree);
  }

  @Test
  public void testListenerInvokedOnlyOnceForMultipleSetRootAsync() {
    mComponentTree.setNewLayoutStateReadyListener(mListener);
    mComponentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);
    mComponentTree.setSizeSpecAsync(mWidthSpec2, mHeightSpec2);

    verify(mListener, never()).onNewLayoutStateReady(any(ComponentTree.class));

    // Now the background thread run the queued task.
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(mListener).onNewLayoutStateReady(mComponentTree);
  }

  @Test
  public void testListenerInvokedForSetRootAsyncWhenAttached() {
    mComponentTree.setNewLayoutStateReadyListener(mListener);
    mComponentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);
    mComponentTree.setLithoView(new LithoView(mContext));
    mComponentTree.attach();

    verify(mListener, never()).onNewLayoutStateReady(any(ComponentTree.class));

    // Now the background thread run the queued task.
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(mListener).onNewLayoutStateReady(mComponentTree);
  }

  @Test
  public void testListenerInvokedForMeasure() {
    mComponentTree.setNewLayoutStateReadyListener(mListener);
    mComponentTree.setLithoView(new LithoView(mContext));
    mComponentTree.attach();
    mComponentTree.setSizeSpec(mWidthSpec, mHeightSpec);

    verify(mListener).onNewLayoutStateReady(mComponentTree);
    reset(mListener);

    mComponentTree.measure(mWidthSpec2, mHeightSpec2, new int[] {0, 0}, false);

    verify(mListener).onNewLayoutStateReady(mComponentTree);
  }

  @Test
  public void testListenerNotInvokedWhenMeasureDoesntComputeALayout() {
    mComponentTree.setNewLayoutStateReadyListener(mListener);
    mComponentTree.setLithoView(new LithoView(mContext));
    mComponentTree.attach();
    mComponentTree.setSizeSpec(mWidthSpec, mHeightSpec);

    verify(mListener).onNewLayoutStateReady(mComponentTree);
    reset(mListener);

    mComponentTree.measure(mWidthSpec, mHeightSpec, new int[] {0, 0}, false);

    verify(mListener, never()).onNewLayoutStateReady(mComponentTree);
  }

  @Test
  public void testListenerNotInvokedWhenNewMeasureSpecsAreCompatible() {
    mComponentTree.setLithoView(new LithoView(mContext));
    mComponentTree.attach();
    mComponentTree.setSizeSpec(mWidthSpec, mHeightSpec);
    mComponentTree.setNewLayoutStateReadyListener(mListener);

    mComponentTree.setSizeSpec(mWidthSpec, mHeightSpec);

    verify(mListener, never()).onNewLayoutStateReady(mComponentTree);
  }
}
