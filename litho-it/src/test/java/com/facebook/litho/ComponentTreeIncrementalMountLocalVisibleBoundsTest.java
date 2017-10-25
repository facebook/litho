/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeIncrementalMountLocalVisibleBoundsTest {
  private LithoView mLithoView;
  private ComponentTree mComponentTree;

  private final Rect mMountedRect = new Rect();

  @Before
  public void setup() {
    ComponentsConfiguration.incrementalMountUsesLocalVisibleBounds = true;
    ComponentContext context = new ComponentContext(RuntimeEnvironment.application);
    mComponentTree =
        ComponentTree.create(
                context, TestDrawableComponent.create(context).color(Color.BLACK).build())
            .layoutDiffing(false)
            .build();

    mLithoView = mock(TestLithoView.class);
    when(mLithoView.getMountState()).thenReturn(mock(MountState.class));
    Whitebox.setInternalState(mComponentTree, "mLithoView", mLithoView);

    // Can't use verify as the rect is reset when it is released back to the pool, which occurs
    // before we can check it.
    doAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                mMountedRect.set((Rect) invocation.getArguments()[1]);
                return null;
              }
            })
        .when(mLithoView)
        .mount(any(LayoutState.class), any(Rect.class), eq(true));
  }

  @After
  public void tearDown() {
    ComponentsConfiguration.incrementalMountUsesLocalVisibleBounds = false;
  }

  @Test
  public void testGetLocalVisibleBounds() {
    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                rect.set(new Rect(10, 5, 20, 15));
                return true;
              }
            })
        .when(mLithoView)
        .getLocalVisibleRect(any(Rect.class));

    mComponentTree.incrementalMountComponent();
    assertThat(mMountedRect).isEqualTo(new Rect(10, 5, 20, 15));
  }

  @Test
  public void testViewPagerInHierarchy() {
    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return false;
              }
            })
        .when(mLithoView)
        .getLocalVisibleRect(any(Rect.class));

    ViewPager viewPager = mock(ViewPager.class);
    when(mLithoView.getParent()).thenReturn(viewPager);

    mComponentTree.attach();

    ArgumentCaptor<ViewPager.OnPageChangeListener> listenerArgumentCaptor =
        ArgumentCaptor.forClass(ViewPager.OnPageChangeListener.class);
    verify(viewPager).addOnPageChangeListener(listenerArgumentCaptor.capture());

    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                rect.set(new Rect(10, 5, 20, 15));
                return true;
              }
            })
        .when(mLithoView)
        .getLocalVisibleRect(any(Rect.class));

    listenerArgumentCaptor.getValue().onPageScrolled(10, 10, 10);
    assertThat(mMountedRect).isEqualTo(new Rect(10, 5, 20, 15));

    mComponentTree.detach();

    ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(viewPager).postOnAnimation(runnableArgumentCaptor.capture());

    runnableArgumentCaptor.getValue().run();
    verify(viewPager).removeOnPageChangeListener(listenerArgumentCaptor.getValue());
  }

  /**
   * Required in order to ensure that {@link LithoView#mount(LayoutState, Rect, boolean)} is mocked
   * correctly (it needs protected access to be mocked).
   */
  public static class TestLithoView extends LithoView {

    public TestLithoView(Context context) {
      super(context);
    }

    protected void mount(
        LayoutState layoutState, Rect currentVisibleArea, boolean processVisibilityOutputs) {
      super.mount(layoutState, currentVisibleArea, processVisibilityOutputs);
    }
  }
}
