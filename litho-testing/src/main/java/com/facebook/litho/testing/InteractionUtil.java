/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import android.app.Instrumentation;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import com.facebook.litho.LithoView;
import com.facebook.litho.LithoViewTestHelper;
import com.facebook.litho.TestItem;

/**
 * Utilities for interacting with an app.
 */
public class InteractionUtil {

  public static void scrollTo(final RecyclerView recyclerView, final int targetScrollY) {
    final int MAX_ITERATIONS = 100;

    int iterations = 0;
    while (targetScrollY != recyclerView.computeVerticalScrollOffset()) {
      if (iterations > MAX_ITERATIONS) {
        throw new RuntimeException(
            "Timed out trying to get to the correct scroll position! target: " + targetScrollY +
                ", final: " + recyclerView.computeVerticalScrollOffset());
      }

      InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
        @Override
        public void run() {
          recyclerView.smoothScrollBy(
              0,
              targetScrollY - recyclerView.computeVerticalScrollOffset());
        }
      });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();

      // Sleep because waitForIdleSync doesn't factor in animations (e.g. the scroll animation) that
      // go through Choreographer
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      iterations++;
    }
  }

  public static void click(View view) {
    final int[] locationOnScreen = new int[2];
    view.getLocationOnScreen(locationOnScreen);

    click(new Point(
        locationOnScreen[0] + view.getWidth() / 2,
        locationOnScreen[1] + view.getHeight() / 2));
  }

  public static void click(LithoView lithoView, String testKey) {
    final TestItem testItem = LithoViewTestHelper.findTestItem(lithoView, testKey);
    if (testItem == null) {
      throw new ViewForTestKeyNotFoundException(testKey);
    }

    final Rect testItemBounds = testItem.getBounds();
    final int[] locationOnScreen = new int[2];
    lithoView.getLocationOnScreen(locationOnScreen);

    click(new Point(
        locationOnScreen[0] + testItemBounds.centerX(),
        locationOnScreen[1] + testItemBounds.centerY()));
  }

  public static void clickBottom(LithoView lithoView, String testKey) {
    final TestItem testItem = LithoViewTestHelper.findTestItem(lithoView, testKey);
    final Rect testItemBounds = testItem.getBounds();
    final int[] locationOnScreen = new int[2];
    lithoView.getLocationOnScreen(locationOnScreen);

    click(new Point(
        locationOnScreen[0] + testItemBounds.centerX(),
        locationOnScreen[1] + testItemBounds.bottom - 1));
  }

  public static void click(Point location) {
    final long time = SystemClock.uptimeMillis();
    final MotionEvent actionDownEvent =
        MotionEvent.obtain(
            time,
            time,
            MotionEvent.ACTION_DOWN,
            location.x,
            location.y,
            0);
    final MotionEvent actionUpEvent =
        MotionEvent.obtain(
            time + 100,
            time + 100,
            MotionEvent.ACTION_UP,
            location.x,
            location.y,
            0);

    final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    instrumentation.sendPointerSync(actionDownEvent);
    instrumentation.sendPointerSync(actionUpEvent);

    instrumentation.waitForIdleSync();
  }
}
