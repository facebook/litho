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
import static android.view.MotionEvent.ACTION_UP;
import static android.view.MotionEvent.obtain;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.OnClickCallbackComponent;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class TouchExpansionDelegateTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = mLithoViewRule.getContext();
  }

  @Test
  public void onTouchEventOnEmptyDelegate_shouldNoOp() {
    final ComponentHost host = new ComponentHost(getApplicationContext());
    final TouchExpansionDelegate delegate = new TouchExpansionDelegate(host);
    final boolean handled =
        delegate.onTouchEvent(
            MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN,
                0,
                0,
                0));

    assertThat(handled)
        .describedAs("TouchEvent on empty delegate should not handle event")
        .isFalse();
  }

  @Test
  public void onTouchEventWithinBounds_shouldBeHandled() {
    final ClickListenerCallback callback = new ClickListenerCallback();
    final Component component =
        Column.create(mContext)
            .child(
                OnClickCallbackComponent.create(mContext)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    emulateClickEvent(mLithoViewRule.getLithoView(), 7, 7);

    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should be handled")
        .isTrue();

    assertThat(callback.count)
        .describedAs("TouchEvent within bounds bounds should be handled only once")
        .isEqualTo(1);
  }

  @Test
  public void onTouchEventOutsideBounds_shouldNotBeHandled() {
    final ClickListenerCallback callback = new ClickListenerCallback();
    final Component component =
        Column.create(mContext)
            .child(
                OnClickCallbackComponent.create(mContext)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    emulateClickEvent(mLithoViewRule.getLithoView(), 2, 2);

    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse();
  }

  @Test
  public void onTouchEventOnUnmount_shouldNotBeHandled() {
    final ClickListenerCallback callback = new ClickListenerCallback();
    final Component component =
        Column.create(mContext)
            .child(
                OnClickCallbackComponent.create(mContext)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    assertThat(mLithoViewRule.getLithoView().getTouchExpansionDelegate().size())
        .describedAs("touch expansion delegates should be present")
        .isEqualTo(1);

    mLithoViewRule.getLithoView().unmountAllItems();

    emulateClickEvent(mLithoViewRule.getLithoView(), 7, 7);

    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse();

    assertThat(mLithoViewRule.getLithoView().getTouchExpansionDelegate().size())
        .describedAs("all touch expansion delegates should be released")
        .isEqualTo(0);
  }

  @Test
  public void onTouchEventOnUpdatedComponentWithoutTouchExpansion_shouldNotBeHandled() {
    final ClickListenerCallback callback = new ClickListenerCallback();
    final Component component =
        Column.create(mContext)
            .child(
                OnClickCallbackComponent.create(mContext)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    final Component updated =
        Column.create(mContext)
            .child(
                OnClickCallbackComponent.create(mContext)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback))
            .paddingPx(YogaEdge.ALL, 10)
            .build();

    mLithoViewRule.setRoot(updated);

    emulateClickEvent(mLithoViewRule.getLithoView(), 7, 7);

    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse();

    emulateClickEvent(mLithoViewRule.getLithoView(), 11, 11);

    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should be handled")
        .isTrue();

    assertThat(callback.count)
        .describedAs("TouchEvent within bounds bounds should be handled only once")
        .isEqualTo(1);

    mLithoViewRule.getLithoView().unmountAllItems();

    assertThat(mLithoViewRule.getLithoView().getTouchExpansionDelegate().size())
        .describedAs("all touch expansion delegates should be released")
        .isEqualTo(0);
  }

  @Test
  public void onTouchEventOnComponentMoved_shouldBeHandled() {
    final ClickListenerCallback callback = new ClickListenerCallback();
    final Component component =
        Column.create(mContext)
            .child(
                OnClickCallbackComponent.create(mContext)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    final Component updated =
        Column.create(mContext)
            .child(Text.create(mContext).text("hello world").widthPx(10).heightPx(10))
            .child(
                OnClickCallbackComponent.create(mContext)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build();

    mLithoViewRule.setRoot(updated).measure().layout();

    emulateClickEvent(mLithoViewRule.getLithoView(), 7, 7);

    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse();

    emulateClickEvent(mLithoViewRule.getLithoView(), 7, 21);

    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should be handled")
        .isTrue();

    assertThat(callback.count)
        .describedAs("TouchEvent within bounds bounds should be handled only once")
        .isEqualTo(1);
  }

  public static void emulateClickEvent(View view, int x, int y) {
    MotionEvent down = obtain(uptimeMillis(), uptimeMillis(), ACTION_DOWN, x, y, 0);
    MotionEvent up = obtain(uptimeMillis() + 10, uptimeMillis() + 10, ACTION_UP, x, y, 0);
    view.dispatchTouchEvent(down);
    view.dispatchTouchEvent(up);
  }

  public static class ClickListenerCallback implements View.OnClickListener {
    boolean handled = false;
    int count = 0;

    @Override
    public void onClick(View v) {
      handled = true;
      count++;
    }

    void reset() {
      handled = false;
      count = 0;
    }
  }
}
