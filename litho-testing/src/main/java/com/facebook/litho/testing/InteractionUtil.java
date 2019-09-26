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

package com.facebook.litho.testing;

import android.app.Instrumentation;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.InstrumentationRegistry;
import com.facebook.litho.LithoView;
import com.facebook.litho.LithoViewTestHelper;
import com.facebook.litho.TestItem;

/** Utilities for interacting with an app. */
public class InteractionUtil {

  /**
   * @deprecated as it is based on {@link RecyclerView#computeVerticalScrollOffset()} which only
   *     returns estimated scroll position, which may lead to the method hanging forever (or until
   *     hitting max iterations count) Please, consider using {@link Scroller} instead
   */
  @Deprecated
  public static void scrollTo(final RecyclerView recyclerView, final int targetScrollY) {
    final int MAX_ITERATIONS = 100;

    int iterations = 0;
    while (targetScrollY != recyclerView.computeVerticalScrollOffset()) {
      if (iterations > MAX_ITERATIONS) {
        throw new RuntimeException(
            "Timed out trying to get to the correct scroll position! target: "
                + targetScrollY
                + ", final: "
                + recyclerView.computeVerticalScrollOffset());
      }

      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              new Runnable() {
                @Override
                public void run() {
                  recyclerView.smoothScrollBy(
                      0, targetScrollY - recyclerView.computeVerticalScrollOffset());
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

    click(
        new Point(
            locationOnScreen[0] + view.getWidth() / 2, locationOnScreen[1] + view.getHeight() / 2));
  }

  public static void click(LithoView lithoView, String testKey) {
    final TestItem testItem = LithoViewTestHelper.findTestItem(lithoView, testKey);
    if (testItem == null) {
      throw new ViewForTestKeyNotFoundException(testKey);
    }

    final Rect testItemBounds = testItem.getBounds();
    final int[] locationOnScreen = new int[2];
    lithoView.getLocationOnScreen(locationOnScreen);

    click(
        new Point(
            locationOnScreen[0] + testItemBounds.centerX(),
            locationOnScreen[1] + testItemBounds.centerY()));
  }

  public static void clickBottom(LithoView lithoView, String testKey) {
    final TestItem testItem = LithoViewTestHelper.findTestItem(lithoView, testKey);
    final Rect testItemBounds = testItem.getBounds();
    final int[] locationOnScreen = new int[2];
    lithoView.getLocationOnScreen(locationOnScreen);

    click(
        new Point(
            locationOnScreen[0] + testItemBounds.centerX(),
            locationOnScreen[1] + testItemBounds.bottom - 1));
  }

  public static void click(Point location) {
    final long time = SystemClock.uptimeMillis();
    final MotionEvent actionDownEvent =
        MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, location.x, location.y, 0);
    final MotionEvent actionUpEvent =
        MotionEvent.obtain(
            time + 100, time + 100, MotionEvent.ACTION_UP, location.x, location.y, 0);

    final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    instrumentation.sendPointerSync(actionDownEvent);
    instrumentation.sendPointerSync(actionUpEvent);

    instrumentation.waitForIdleSync();
  }

  /**
   * Class that keeps track of the scroll position of provided {@link RecyclerView}, and provides
   * APIs to request scroll to a certain position
   */
  public static class Scroller extends RecyclerView.OnScrollListener {
    private static final int MAX_ITERATIONS = 100;

    private final RecyclerView mRecyclerView;
    private int mScrollPositionX;
    private int mScrollPositionY;

    public Scroller(RecyclerView recyclerView) {
      mRecyclerView = recyclerView;
      recyclerView.addOnScrollListener(this);
    }

    @Override
    public void onScrolled(RecyclerView rv, int dx, int dy) {
      super.onScrolled(rv, dx, dy);
      mScrollPositionX += dx;
      mScrollPositionY += dy;
    }

    public void scrollToPositionX(int targetPositionX) {
      scrollTo(true, targetPositionX);
    }

    public void scrollToPositionY(int targetPositionY) {
      scrollTo(false, targetPositionY);
    }

    private void scrollTo(final boolean xAxis, final int targetPosition) {
      int iterations = 0;
      while (targetPosition != getScrollPosition(xAxis)) {
        if (iterations > MAX_ITERATIONS) {
          throw new RuntimeException(
              "Timed out trying to get to the correct scroll position! target: "
                  + targetPosition
                  + ", final: "
                  + getScrollPosition(xAxis));
        }

        InstrumentationRegistry.getInstrumentation()
            .runOnMainSync(
                new Runnable() {
                  @Override
                  public void run() {
                    mRecyclerView.smoothScrollBy(0, targetPosition - getScrollPosition(xAxis));
                  }
                });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Sleep because waitForIdleSync doesn't factor in animations (e.g. the scroll animation)
        // that
        // go through Choreographer
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        iterations++;
      }
    }

    private int getScrollPosition(boolean xAxis) {
      return xAxis ? mScrollPositionX : mScrollPositionY;
    }
  }
}
