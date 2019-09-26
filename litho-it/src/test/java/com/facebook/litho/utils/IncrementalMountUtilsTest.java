/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.utils;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

/** Tests {@link IncrementalMountUtils} */
@RunWith(ComponentsTestRunner.class)
public class IncrementalMountUtilsTest {
  private static final int SCROLLING_VIEW_WIDTH = 100;
  private static final int SCROLLING_VIEW_HEIGHT = 1000;

  public TestWrapperView mWrapperView = mock(TestWrapperView.class);
  public LithoView mLithoView = mock(LithoView.class);
  public ViewGroup mViewGroup = mock(ViewGroup.class);

  private final Rect mMountedRect = new Rect();

  @Before
  public void setUp() {
    when(mLithoView.isIncrementalMountEnabled()).thenReturn(true);
    when(mWrapperView.getWrappedView()).thenReturn(mLithoView);

    when(mViewGroup.getChildCount()).thenReturn(1);
    when(mViewGroup.getChildAt(0)).thenReturn(mLithoView);
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
            })
        .when(mLithoView)
        .performIncrementalMount(any(Rect.class), eq(true));
  }

  @Test
  public void testIncrementalMountForLithoViewVisibleAtTop() {
    setupViewBounds(mLithoView, 0, -10, SCROLLING_VIEW_WIDTH, 10);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 10, SCROLLING_VIEW_WIDTH, 20));
  }

  @Test
  public void testIncrementalMountForLithoViewVisibleAtTopWithTranslationYPartialIn() {
    setupViewBounds(mLithoView, 0, -10, SCROLLING_VIEW_WIDTH, 10);
    setupViewTranslations(mLithoView, 0, 5);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 5, SCROLLING_VIEW_WIDTH, 20));
  }

  @Test
  public void testIncrementalMountForLithoViewVisibleAtTopWithTranslationYFullyOut() {
    setupViewBounds(mLithoView, 0, -10, SCROLLING_VIEW_WIDTH, 10);
    setupViewTranslations(mLithoView, 0, -15);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mLithoView, never()).performIncrementalMount(any(Rect.class), eq(true));
  }

  @Test
  public void testIncrementalMountForLithoViewVisibleAtLeft() {
    setupViewBounds(mLithoView, -10, 0, 10, SCROLLING_VIEW_HEIGHT);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(10, 0, 20, SCROLLING_VIEW_HEIGHT));
  }

  @Test
  public void testIncrementalMountForLithoViewVisibleAtLeftWithTranslationXFullyIn() {
    setupViewBounds(mLithoView, -10, 0, 10, SCROLLING_VIEW_HEIGHT);
    setupViewTranslations(mLithoView, 15, 0);
    setupLithoViewPreviousBounds(mLithoView, 20, SCROLLING_VIEW_HEIGHT);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mLithoView, never()).performIncrementalMount(any(Rect.class), eq(true));
  }

  @Test
  public void testIncrementalMountForLithoViewVisibleAtLeftWithTranslationXPartialOut() {
    setupViewBounds(mLithoView, -10, 0, 10, SCROLLING_VIEW_HEIGHT);
    setupViewTranslations(mLithoView, -7, 0);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(17, 0, 20, SCROLLING_VIEW_HEIGHT));
  }

  @Test
  public void testIncrementalMountForLithoViewVisibleAtBottom() {
    setupViewBounds(
        mLithoView, 0, SCROLLING_VIEW_HEIGHT - 5, SCROLLING_VIEW_WIDTH, SCROLLING_VIEW_HEIGHT + 5);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, SCROLLING_VIEW_WIDTH, 5));
  }

  @Test
  public void testIncrementalMountForLithoViewVisibleAtRight() {
    setupViewBounds(
        mLithoView, SCROLLING_VIEW_WIDTH - 5, 0, SCROLLING_VIEW_WIDTH + 5, SCROLLING_VIEW_HEIGHT);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, 5, SCROLLING_VIEW_HEIGHT));
  }

  @Test
  public void testIncrementalMountForLithoViewNewlyFullyVisible() {
    setupViewBounds(mLithoView, 0, 10, SCROLLING_VIEW_WIDTH, 20);
    setupLithoViewPreviousBounds(mLithoView, SCROLLING_VIEW_WIDTH, 5);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, SCROLLING_VIEW_WIDTH, 10));
  }

  @Test
  public void testIncrementalMountForLithoViewAlreadyFullyVisible() {
    setupViewBounds(mLithoView, 0, 10, SCROLLING_VIEW_WIDTH, 20);
    setupLithoViewPreviousBounds(mLithoView, SCROLLING_VIEW_WIDTH, 10);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mLithoView, never()).performIncrementalMount(any(Rect.class), eq(true));
  }

  @Test
  public void testNoIncrementalMountWhenNotEnabled() {
    setupViewBounds(
        mLithoView, 0, SCROLLING_VIEW_HEIGHT - 5, SCROLLING_VIEW_WIDTH, SCROLLING_VIEW_HEIGHT + 5);
    when(mLithoView.isIncrementalMountEnabled()).thenReturn(false);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mLithoView, never()).performIncrementalMount(any(Rect.class), eq(true));
  }

  @Test
  public void testIncrementalMountForWrappedViewAtTop() {
    when(mViewGroup.getChildAt(0)).thenReturn(mWrapperView);
    setupViewBounds(mWrapperView, 0, -10, SCROLLING_VIEW_WIDTH, 10);
    setupViewBounds(mLithoView, 0, 0, SCROLLING_VIEW_WIDTH, 20);

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
    setupViewBounds(mLithoView, 0, 0, SCROLLING_VIEW_WIDTH, 10);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, SCROLLING_VIEW_WIDTH, 5));
  }

  @Test
  public void testIncrementalMountForWrappedLithoViewNewlyFullyVisible() {
    when(mViewGroup.getChildAt(0)).thenReturn(mWrapperView);
    setupViewBounds(mWrapperView, 0, 10, SCROLLING_VIEW_WIDTH, 20);
    setupViewBounds(mLithoView, 0, 0, SCROLLING_VIEW_WIDTH, 10);
    setupLithoViewPreviousBounds(mLithoView, SCROLLING_VIEW_WIDTH, 5);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verifyPerformIncrementalMountCalled(new Rect(0, 0, SCROLLING_VIEW_WIDTH, 10));
  }

  @Test
  public void testIncrementalMountForWrappedLithoViewAlreadyFullyVisible() {
    when(mViewGroup.getChildAt(0)).thenReturn(mWrapperView);
    setupViewBounds(mWrapperView, 0, 10, SCROLLING_VIEW_WIDTH, 20);
    setupViewBounds(mLithoView, 0, 0, SCROLLING_VIEW_WIDTH, 10);
    setupLithoViewPreviousBounds(mLithoView, SCROLLING_VIEW_WIDTH, 10);

    IncrementalMountUtils.performIncrementalMount(mViewGroup);

    verify(mLithoView, never()).performIncrementalMount(any(Rect.class), eq(true));
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

  private static void setupLithoViewPreviousBounds(LithoView lithoView, int width, int height) {
    when(lithoView.getPreviousMountBounds()).thenReturn(new Rect(0, 0, width, height));
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
