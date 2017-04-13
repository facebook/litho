/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.utils;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.LithoView;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.utils.IncrementalMountUtils.WrapperView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link IncrementalMountUtils}
 */
@RunWith(ComponentsTestRunner.class)
public class IncrementalMountUtilsTest {
  private static final int SCROLLING_VIEW_WIDTH = 100;
  private static final int SCROLLING_VIEW_HEIGHT = 1000;

  public TestWrapperView mWrapperView = mock(TestWrapperView.class);
  public LithoView mComponentView = mock(LithoView.class);
  public ViewGroup mViewGroup = mock(ViewGroup.class);

  private final Rect mMountedRect = new Rect();

  @Before
  public void setUp() {
    when(mComponentView.isIncrementalMountEnabled()).thenReturn(true);
    when(mWrapperView.getWrappedView()).thenReturn(mComponentView);

    when(mViewGroup.getChildCount()).thenReturn(1);
    when(mViewGroup.getChildAt(0)).thenReturn(mComponentView);
    when(mViewGroup.getWidth()).thenReturn(SCROLLING_VIEW_WIDTH);
    when(mViewGroup.getHeight()).thenReturn(SCROLLING_VIEW_HEIGHT);

    // Can't use verify as the rect is reset when it is released back to the pool, which occurs
    // before we can check it.
    doAnswer(
        new Answer() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            mMountedRect.set((Rect) invocation.getArguments()[0]);
            return null;
          }
        }).when(mComponentView).performIncrementalMount(any(Rect.class));
  }

  @Test
  public void testIncrementalMountForComponentViewVisibleAtTop() {
    setupViewBounds(mComponentView, 0, -10, SCROLLING_VIEW_WIDTH, 10);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 10, SCROLLING_VIEW_WIDTH, 20));
  }

  @Test
  public void testIncrementalMountForComponentViewVisibleAtTopWithTranslationYPartialIn() {
    setupViewBounds(mComponentView, 0, -10, SCROLLING_VIEW_WIDTH, 10);
    setupViewTranslations(mComponentView, 0, 5);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 5, SCROLLING_VIEW_WIDTH, 20));
  }

  @Test
  public void testIncrementalMountForComponentViewVisibleAtTopWithTranslationYFullyOut() {
    setupViewBounds(mComponentView, 0, -10, SCROLLING_VIEW_WIDTH, 10);
    setupViewTranslations(mComponentView, 0, -15);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mComponentView, never()).performIncrementalMount(any(Rect.class));
  }

  @Test
  public void testIncrementalMountForComponentViewVisibleAtLeft() {
    setupViewBounds(mComponentView, -10, 0, 10, SCROLLING_VIEW_HEIGHT);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(10, 0, 20, SCROLLING_VIEW_HEIGHT));
  }

  @Test
  public void testIncrementalMountForComponentViewVisibleAtLeftWithTranslationXFullyIn() {
    setupViewBounds(mComponentView, -10, 0, 10, SCROLLING_VIEW_HEIGHT);
    setupViewTranslations(mComponentView, 15, 0);
    setupComponentViewPreviousBounds(mComponentView, 20, SCROLLING_VIEW_HEIGHT);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mComponentView, never()).performIncrementalMount(any(Rect.class));
  }

  @Test
  public void testIncrementalMountForComponentViewVisibleAtLeftWithTranslationXPartialOut() {
    setupViewBounds(mComponentView, -10, 0, 10, SCROLLING_VIEW_HEIGHT);
    setupViewTranslations(mComponentView, -7, 0);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(17, 0, 20, SCROLLING_VIEW_HEIGHT));
  }

  @Test
  public void testIncrementalMountForComponentViewVisibleAtBottom() {
    setupViewBounds(
        mComponentView,
        0,
        SCROLLING_VIEW_HEIGHT - 5,
        SCROLLING_VIEW_WIDTH,
        SCROLLING_VIEW_HEIGHT + 5);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, SCROLLING_VIEW_WIDTH, 5));
  }

  @Test
  public void testIncrementalMountForComponentViewVisibleAtRight() {
    setupViewBounds(
        mComponentView,
        SCROLLING_VIEW_WIDTH - 5,
        0,
        SCROLLING_VIEW_WIDTH + 5,
        SCROLLING_VIEW_HEIGHT);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, 5, SCROLLING_VIEW_HEIGHT));
  }

  @Test
  public void testIncrementalMountForComponentViewNewlyFullyVisible() {
    setupViewBounds(mComponentView, 0, 10, SCROLLING_VIEW_WIDTH, 20);
    setupComponentViewPreviousBounds(mComponentView, SCROLLING_VIEW_WIDTH, 5);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, SCROLLING_VIEW_WIDTH, 10));
  }

  @Test
  public void testIncrementalMountForComponentViewAlreadyFullyVisible() {
    setupViewBounds(mComponentView, 0, 10, SCROLLING_VIEW_WIDTH, 20);
    setupComponentViewPreviousBounds(mComponentView, SCROLLING_VIEW_WIDTH, 10);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mComponentView, never()).performIncrementalMount(any(Rect.class));
  }

  @Test
  public void testNoIncrementalMountWhenNotEnabled() {
    setupViewBounds(
        mComponentView,
        0,
        SCROLLING_VIEW_HEIGHT - 5,
        SCROLLING_VIEW_WIDTH,
        SCROLLING_VIEW_HEIGHT + 5);
    when(mComponentView.isIncrementalMountEnabled()).thenReturn(false);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mComponentView, never()).performIncrementalMount(any(Rect.class));
  }

  @Test
  public void testIncrementalMountForWrappedViewAtTop() {
    when(mViewGroup.getChildAt(0)).thenReturn(mWrapperView);
    setupViewBounds(mWrapperView, 0, -10, SCROLLING_VIEW_WIDTH, 10);
    setupViewBounds(mComponentView, 0, 0, SCROLLING_VIEW_WIDTH, 20);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 10, SCROLLING_VIEW_WIDTH, 20));
  }

  @Test
  public void testIncrementalMountForWrappedViewAtBottom() {
    when(mViewGroup.getChildAt(0)).thenReturn(mWrapperView);
    setupViewBounds(
        mWrapperView,
        0,
        SCROLLING_VIEW_HEIGHT - 5,
        SCROLLING_VIEW_WIDTH,
        SCROLLING_VIEW_HEIGHT + 5);
    setupViewBounds(mComponentView, 0, 0, SCROLLING_VIEW_WIDTH, 10);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, SCROLLING_VIEW_WIDTH, 5));
  }

  @Test
  public void testIncrementalMountForWrappedComponentViewNewlyFullyVisible() {
    when(mViewGroup.getChildAt(0)).thenReturn(mWrapperView);
    setupViewBounds(mWrapperView, 0, 10, SCROLLING_VIEW_WIDTH, 20);
    setupViewBounds(mComponentView, 0, 0, SCROLLING_VIEW_WIDTH, 10);
    setupComponentViewPreviousBounds(mComponentView, SCROLLING_VIEW_WIDTH, 5);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, SCROLLING_VIEW_WIDTH, 10));
  }

  @Test
  public void testIncrementalMountForWrappedComponentViewAlreadyFullyVisible() {
    when(mViewGroup.getChildAt(0)).thenReturn(mWrapperView);
    setupViewBounds(mWrapperView, 0, 10, SCROLLING_VIEW_WIDTH, 20);
    setupViewBounds(mComponentView, 0, 0, SCROLLING_VIEW_WIDTH, 10);
    setupComponentViewPreviousBounds(mComponentView, SCROLLING_VIEW_WIDTH, 10);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mComponentView, never()).performIncrementalMount(any(Rect.class));
  }

  private static void setupViewBounds(View view, int l, int t, int r, int b) {
    when(view.getLeft()).thenReturn(l);
    when(view.getTop()).thenReturn(t);
    when(view.getRight()).thenReturn(r);
    when(view.getBottom()).thenReturn(b);
    when(view.getWidth()).thenReturn(r - l);
    when(view.getHeight()).thenReturn(b - t);
  }

  private static void setupViewTranslations(View view, float translationX, float translationY) {
    when(view.getTranslationX()).thenReturn(translationX);
    when(view.getTranslationY()).thenReturn(translationY);
  }

  private static void setupComponentViewPreviousBounds(
      LithoView componentView,
      int width,
      int height) {
    when(componentView.getPreviousMountBounds()).thenReturn(new Rect(0, 0, width, height));
  }

  private void verifyPerformIncrementalMountCalled(Rect rect) {
    assertThat(mMountedRect).isEqualTo(rect);
  }

  public static class TestWrapperView extends View implements WrapperView {

    public TestWrapperView(Context context) {
      super(context);
    }

    @Override
    public View getWrappedView() {
      return null;
    }
  }
}
