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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.ComponentTree.create;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import android.os.Looper;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    mContext = new ComponentContext(getApplicationContext());
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

    verify(mListener, never()).onNewLayoutStateReady((ComponentTree) any());

    // Now the background thread run the queued task.
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(mListener).onNewLayoutStateReady(mComponentTree);
  }

  @Test
  public void testListenerInvokedOnlyOnceForMultipleSetRootAsync() {
    mComponentTree.setNewLayoutStateReadyListener(mListener);
    mComponentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);
    mComponentTree.setSizeSpecAsync(mWidthSpec2, mHeightSpec2);

    verify(mListener, never()).onNewLayoutStateReady((ComponentTree) any());

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

    verify(mListener, never()).onNewLayoutStateReady((ComponentTree) any());

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
