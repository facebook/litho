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

import static android.os.SystemClock.uptimeMillis;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.obtain;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class TouchExpansionDelegateTest {
  private TouchExpansionDelegate mTouchDelegate;

  @Before
  public void setup() {
    mTouchDelegate = new TouchExpansionDelegate(new ComponentHost(RuntimeEnvironment.application));
  }

  @Test
  public void testEmptyOnTouchEvent() {
    mTouchDelegate.onTouchEvent(
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            0,
            0,
            0));
  }

  @Test
  public void testTouchWithinBounds() {
    final View view = mock(View.class);
    when(view.getContext()).thenReturn(application);
    when(view.getWidth()).thenReturn(4);
    when(view.getHeight()).thenReturn(6);

    mTouchDelegate.registerTouchExpansion(0, view, new Rect(0, 0, 10, 10));

    MotionEvent event = obtain(uptimeMillis(), uptimeMillis(), ACTION_DOWN, 5, 5, 0);

    mTouchDelegate.onTouchEvent(event);

    verify(view, times(1)).dispatchTouchEvent(event);
    assertThat(event.getX()).isEqualTo(2f);
    assertThat(event.getY()).isEqualTo(3f);
  }

  @Test
  public void testTouchOutsideBounds() {
    final View view = mock(View.class);
    when(view.getContext()).thenReturn(RuntimeEnvironment.application);

    mTouchDelegate.registerTouchExpansion(0, view, new Rect(0, 0, 10, 10));

    MotionEvent event =
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            100,
            100,
            0);

    mTouchDelegate.onTouchEvent(event);

    verify(view, never()).dispatchTouchEvent(event);
  }

  @Test
  public void testUnregister() {
    final View view = mock(View.class);
    when(view.getContext()).thenReturn(RuntimeEnvironment.application);

    mTouchDelegate.registerTouchExpansion(0, view, new Rect(0, 0, 10, 10));
    mTouchDelegate.unregisterTouchExpansion(0);

    MotionEvent event =
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            5,
            5,
            0);

    mTouchDelegate.onTouchEvent(event);

    verify(view, never()).dispatchTouchEvent(event);
  }

  @Test
  public void testMove() {
    final View firstView = mock(View.class);
    final View secondView = mock(View.class);

    when(firstView.getContext()).thenReturn(RuntimeEnvironment.application);
    when(secondView.getContext()).thenReturn(RuntimeEnvironment.application);

    mTouchDelegate.registerTouchExpansion(0, firstView, new Rect(0, 0, 10, 10));
    mTouchDelegate.registerTouchExpansion(4, secondView, new Rect(0, 0, 10, 10));
    mTouchDelegate.moveTouchExpansionIndexes(0, 2);

    mTouchDelegate.unregisterTouchExpansion(2);
  }

  @Test
  public void testComplexMove() {
    final View firstView = mock(View.class);
    final View secondView = mock(View.class);

    when(firstView.getContext()).thenReturn(RuntimeEnvironment.application);
    when(secondView.getContext()).thenReturn(RuntimeEnvironment.application);

    mTouchDelegate.registerTouchExpansion(0, firstView, new Rect(0, 0, 10, 10));
    mTouchDelegate.registerTouchExpansion(4, secondView, new Rect(0, 0, 10, 10));
    mTouchDelegate.moveTouchExpansionIndexes(0, 4);
    mTouchDelegate.unregisterTouchExpansion(4);
    mTouchDelegate.unregisterTouchExpansion(4);
  }

  @Test
  public void testDrawingOrder() {
    final View view1 = mock(View.class);
    when(view1.getContext()).thenReturn(RuntimeEnvironment.application);
    when(view1.dispatchTouchEvent(any(MotionEvent.class))).thenReturn(true);
    mTouchDelegate.registerTouchExpansion(0, view1, new Rect(0, 0, 10, 10));

    final View view2 = mock(View.class);
    when(view2.getContext()).thenReturn(RuntimeEnvironment.application);
    when(view2.dispatchTouchEvent(any(MotionEvent.class))).thenReturn(true);
    mTouchDelegate.registerTouchExpansion(1, view2, new Rect(0, 0, 10, 10));

    MotionEvent event =
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            5,
            5,
            0);

    mTouchDelegate.onTouchEvent(event);

    verify(view1, never()).dispatchTouchEvent(event);
    verify(view2, times(1)).dispatchTouchEvent(event);
  }
}
