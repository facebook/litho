/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.ViewGroup;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeIncrementalMountTest {
  private ComponentView mComponentView;
  private ComponentTree mComponentTree;

  private final Rect mMountedRect = new Rect();

  @Before
  public void setup() {
    ComponentContext context = new ComponentContext(RuntimeEnvironment.application);
    mComponentTree = ComponentTree.create(
        context,
        TestDrawableComponent.create(context)
            .color(Color.BLACK)
            .build())
        .build();

    mComponentView = mock(TestComponentView.class);
    Whitebox.setInternalState(mComponentTree, "mComponentView", mComponentView);

    // Can't use verify as the rect is reset when it is released back to the pool, which occurs
    // before we can check it.
    doAnswer(
        new Answer() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            mMountedRect.set((Rect) invocation.getArguments()[1]);
            return null;
          }
        }).when(mComponentView).mount(any(LayoutState.class), any(Rect.class));
  }

  @Test
  public void testIncrementalMountBoundsSameAsParent() {
    setupIncrementalMountTest(new Rect(0, 0, 10, 10), new Rect(0, 0, 10, 10));

    mComponentTree.incrementalMountComponent();

    assertThat(mMountedRect).isEqualTo(new Rect(0, 0, 10, 10));
  }

  @Test
  public void testIncrementalMountBoundsWithNoParent() {
    setupIncrementalMountTest(new Rect(0, 0, 10, 10), null);

    mComponentTree.incrementalMountComponent();
    assertThat(mMountedRect).isEqualTo(new Rect(0, 0, 10, 10));
  }

  @Test
  public void testIncrementalMountBoundsInsideParent() {
    setupIncrementalMountTest(new Rect(10, 10, 20, 20), new Rect(0, 0, 30, 30));

    mComponentTree.incrementalMountComponent();
    assertThat(mMountedRect).isEqualTo(new Rect(0, 0, 10, 10));
  }

  @Test
  public void testIncrementalMountBoundsOutsideParent() {
    setupIncrementalMountTest(new Rect(10, 10, 20, 20), new Rect(20, 10, 30, 30));

    mComponentTree.incrementalMountComponent();
    verify(mComponentView, never()).mount(any(LayoutState.class), any(Rect.class));
  }

  @Test
  public void testIncrementalMountBoundsToLeftOfParent() {
    setupIncrementalMountTest(new Rect(10, 10, 20, 20), new Rect(15, 10, 25, 20));

    mComponentTree.incrementalMountComponent();
    assertThat(mMountedRect).isEqualTo(new Rect(5, 0, 10, 10));
  }

  @Test
  public void testIncrementalMountBoundsToTopOfParent() {
    setupIncrementalMountTest(new Rect(10, 10, 20, 20), new Rect(10, 15, 20, 25));

    mComponentTree.incrementalMountComponent();
    assertThat(mMountedRect).isEqualTo(new Rect(0, 5, 10, 10));
  }

  @Test
  public void testIncrementalMountBoundsToRightOfParent() {
    setupIncrementalMountTest(new Rect(10, 10, 20, 20), new Rect(5, 10, 15, 20));

    mComponentTree.incrementalMountComponent();
    assertThat(mMountedRect).isEqualTo(new Rect(0, 0, 5, 10));
  }

  @Test
  public void testIncrementalMountBoundsToBottomOfParent() {
    setupIncrementalMountTest(new Rect(10, 10, 20, 20), new Rect(10, 5, 20, 15));

    mComponentTree.incrementalMountComponent();
    assertThat(mMountedRect).isEqualTo(new Rect(0, 0, 10, 5));
  }

  private void setupIncrementalMountTest(
      final Rect componentViewBoundsInScreen,
      final Rect parentBoundsInScreen) {

    doAnswer(
        new Answer<Void>() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            int[] location = (int[]) invocation.getArguments()[0];
            location[0] = componentViewBoundsInScreen.left;
            location[1] = componentViewBoundsInScreen.top;
            return null;
          }
        }).when(mComponentView).getLocationOnScreen(any(int[].class));
    when(mComponentView.getWidth())
        .thenReturn(componentViewBoundsInScreen.right - componentViewBoundsInScreen.left);
    when(mComponentView.getHeight())
        .thenReturn(componentViewBoundsInScreen.bottom - componentViewBoundsInScreen.top);

    if (parentBoundsInScreen == null) {
      return;
    }

    ViewGroup parentView = mock(ViewGroup.class);
    when(mComponentView.getParent()).thenReturn(parentView);

    doAnswer(
        new Answer<Void>() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            int[] location = (int[]) invocation.getArguments()[0];
            location[0] = parentBoundsInScreen.left;
            location[1] = parentBoundsInScreen.top;
            return null;
          }
        }).when(parentView).getLocationOnScreen(any(int[].class));
    when(parentView.getWidth()).thenReturn(parentBoundsInScreen.right - parentBoundsInScreen.left);
    when(parentView.getHeight()).thenReturn(parentBoundsInScreen.bottom - parentBoundsInScreen.top);
  }

  /**
   * Required in order to ensure that {@link ComponentView#mount(LayoutState, Rect)} is
   * mocked correctly (it needs protected access to be mocked).
   */
  public static class TestComponentView extends ComponentView {

    public TestComponentView(Context context) {
      super(context);
    }

    protected void mount(
        LayoutState layoutState,
        Rect currentVisibleArea) {
      super.mount(layoutState, currentVisibleArea);
    }
  }
}
