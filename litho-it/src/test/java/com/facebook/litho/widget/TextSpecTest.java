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

package com.facebook.litho.widget;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.eventhandler.EventHandlerTestHelper;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests {@link Text} component.
 */

@RunWith(ComponentsTestRunner.class)
public class TextSpecTest {
  private ComponentContext mContext;

  private static final int FULL_TEXT_WIDTH = 100;
  private static final int MINIMAL_TEXT_WIDTH = 95;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  private static class TestMountableCharSequence implements MountableCharSequence {

    Drawable mountDrawable;

    @Override
    public void onMount(Drawable parent) {
      mountDrawable = parent;
    }

    @Override
    public void onUnmount(Drawable parent) {}

    @Override
    public int length() {
      return 0;
    }

    @Override
    public char charAt(int index) {
      return 0;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return null;
    }

    public Drawable getMountDrawable() {
      return mountDrawable;
    }
  }

  @Test
  public void testTextWithoutClickableSpans() {
    TextDrawable drawable = getMountedDrawableForText("Some text.");
    assertThat(drawable.getClickableSpans()).isNull();
  }

  @Test
  public void testSpannableWithoutClickableSpans() {
    Spannable nonClickableText = Spannable.Factory.getInstance().newSpannable("Some text.");

    TextDrawable drawable = getMountedDrawableForText(nonClickableText);
    assertThat(drawable.getClickableSpans()).isNotNull().hasSize(0);
  }

  @Test
  public void testSpannableWithClickableSpans() {
    Spannable clickableText = Spannable.Factory.getInstance().newSpannable("Some text.");
    clickableText.setSpan(new ClickableSpan() {
      @Override
      public void onClick(View widget) {
      }
    }, 0, 1, 0);

    TextDrawable drawable = getMountedDrawableForText(clickableText);
    assertThat(drawable.getClickableSpans()).isNotNull().hasSize(1);
  }

  @Test(expected = IllegalStateException.class)
  public void testTextIsRequired() throws Exception {
    Text.create(mContext).build();
  }

  @Test
  public void testMountableCharSequenceText() {
    TestMountableCharSequence testMountableCharSequence = new TestMountableCharSequence();
    assertThat(testMountableCharSequence.getMountDrawable()).isNull();
    TextDrawable drawable = getMountedDrawableForText(testMountableCharSequence);
    assertThat(testMountableCharSequence.getMountDrawable()).isSameAs(drawable);
  }

  @Test
  public void testTouchOffsetChangeHandlerFired() {
    final boolean[] eventFired = new boolean[] {false};
    EventHandler<TextOffsetOnTouchEvent> eventHandler =
        EventHandlerTestHelper.createMockEventHandler(
            TextOffsetOnTouchEvent.class,
            new EventHandlerTestHelper.MockEventHandler<TextOffsetOnTouchEvent, Void>() {
              @Override
              public Void handleEvent(TextOffsetOnTouchEvent event) {
                eventFired[0] = true;
                return null;
              }
            });

    LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        Text.create(mContext)
            .text("Some text")
            .textOffsetOnTouchHandler(eventHandler)
            .build());
    TextDrawable textDrawable = (TextDrawable) (lithoView.getDrawables().get(0));
    MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
    boolean handled = textDrawable.onTouchEvent(motionEvent, lithoView);
    // We don't consume touch events from TextTouchOffsetChange event
    assertThat(handled).isFalse();
    assertThat(eventFired[0]).isTrue();
  }

  @Test
  public void testTouchOffsetChangeHandlerNotFired() {
    final boolean[] eventFired = new boolean[] {false};
    EventHandler<TextOffsetOnTouchEvent> eventHandler =
        EventHandlerTestHelper.createMockEventHandler(
            TextOffsetOnTouchEvent.class,
            new EventHandlerTestHelper.MockEventHandler<TextOffsetOnTouchEvent, Void>() {
              @Override
              public Void handleEvent(TextOffsetOnTouchEvent event) {
                eventFired[0] = true;
                return null;
              }
            });

    LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        Text.create(mContext)
            .text("Text2")
            .textOffsetOnTouchHandler(eventHandler)
            .build());

    TextDrawable textDrawable = (TextDrawable) (lithoView.getDrawables().get(0));

    MotionEvent actionUp = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0);
    boolean handledActionUp = textDrawable.onTouchEvent(actionUp, lithoView);
    assertThat(handledActionUp).isFalse();
    assertThat(eventFired[0]).isFalse();

    MotionEvent actionDown = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 0, 0, 0);
    boolean handledActionMove = textDrawable.onTouchEvent(actionDown, lithoView);
    assertThat(handledActionMove).isFalse();
    assertThat(eventFired[0]).isFalse();
  }

  @Test
  public void testColorDefault() {
    TextDrawable drawable = getMountedDrawableForText("Some text");
    assertThat(drawable.getColor()).isEqualTo(Color.BLACK);
  }

  @Test
  public void testColorOverride() {
    int[][] states = {{0}};
    int[] colors = {Color.GREEN};
    ColorStateList colorStateList = new ColorStateList(states, colors);
    TextDrawable drawable = getMountedDrawableForTextWithColors(
        "Some text",
        Color.RED,
        colorStateList);
    assertThat(drawable.getColor()).isEqualTo(Color.RED);
  }

  @Test
  public void testColor() {
    TextDrawable drawable = getMountedDrawableForTextWithColors(
        "Some text",
        Color. RED,
        null);
    assertThat(drawable.getColor()).isEqualTo(Color.RED);
  }

  @Test
  public void testColorStateList() {
    int[][] states = {{0}};
    int[] colors = {Color.GREEN};
    ColorStateList colorStateList = new ColorStateList(states, colors);
    TextDrawable drawable = getMountedDrawableForTextWithColors(
        "Some text",
        0,
        colorStateList);
    assertThat(drawable.getColor()).isEqualTo(Color.GREEN);
  }

  @Test
  public void testColorStateListMultipleStates() {
    ColorStateList colorStateList = new ColorStateList(
        new int[][]{
            new int[]{-android.R.attr.state_enabled}, //disabled state
            new int[]{}
        },
        new int[] {
            Color.RED,
            Color.GREEN
        }
    );
    TextDrawable drawable = getMountedDrawableForTextWithColors(
        "Some text",
        0,
        colorStateList);

    //color should fallback to default state
    assertThat(drawable.getColor()).isEqualTo(Color.GREEN);
  }

  private TextDrawable getMountedDrawableForText(CharSequence text) {
    return (TextDrawable) ComponentTestHelper.mountComponent(
        mContext,
        Text.create(mContext)
            .text(text)
            .build())
        .getDrawables()
        .get(0);
  }

  private TextDrawable getMountedDrawableForTextWithColors(
      CharSequence text,
      int color,
      ColorStateList colorStateList) {
    Text.Builder builder = Text.create(mContext).text(text);
    if (color != 0) {
      builder.textColor(color);
    }
    if (colorStateList != null) {
      builder.textColorStateList(colorStateList);
    }
    return (TextDrawable) ComponentTestHelper.mountComponent(
          mContext, builder.build())
        .getDrawables()
        .get(0);
  }

  @Test
  public void testSynchronizedTypefaceSparseArray() {
    SparseArray<Typeface> sparseArray = new SparseArray<>();
    sparseArray.put(1, Typeface.DEFAULT);
    SynchronizedTypefaceHelper.SynchronizedTypefaceSparseArray synchronizedSparseArray =
        new SynchronizedTypefaceHelper.SynchronizedTypefaceSparseArray(sparseArray);
    synchronizedSparseArray.put(2, Typeface.DEFAULT_BOLD);
    assertThat(synchronizedSparseArray.get(1)).isSameAs(Typeface.DEFAULT);
    assertThat(synchronizedSparseArray.get(2)).isSameAs(Typeface.DEFAULT_BOLD);
  }

  @Test
  public void testSynchronizedLongSparseArray() {
    SynchronizedTypefaceHelper.SynchronizedLongSparseArray synchronizedLongSparseArray =
        new SynchronizedTypefaceHelper.SynchronizedLongSparseArray(new Object(), 2);
    SparseArray<Typeface> sparseArray = new SparseArray<>();
    sparseArray.put(1, Typeface.DEFAULT);
    synchronizedLongSparseArray.put(2, sparseArray);
    SparseArray<Typeface> gotSparseArray = synchronizedLongSparseArray.get(2);
    assertThat(gotSparseArray)
        .isInstanceOf(SynchronizedTypefaceHelper.SynchronizedTypefaceSparseArray.class);
    assertThat(gotSparseArray.get(1)).isSameAs(Typeface.DEFAULT);
  }

  @Test
  public void testSynchronizedSparseArray() {
    SynchronizedTypefaceHelper.SynchronizedSparseArray synchronizedSparseArray =
        new SynchronizedTypefaceHelper.SynchronizedSparseArray(new Object(), 2);
    SparseArray<Typeface> sparseArray = new SparseArray<>();
    sparseArray.put(1, Typeface.DEFAULT);
    synchronizedSparseArray.put(2, sparseArray);
    SparseArray<Typeface> gotSparseArray = synchronizedSparseArray.get(2);
    assertThat(gotSparseArray)
        .isInstanceOf(SynchronizedTypefaceHelper.SynchronizedTypefaceSparseArray.class);
    assertThat(gotSparseArray.get(1)).isSameAs(Typeface.DEFAULT);
  }

  @Test
  public void testFullWidthText() {
    final Layout layout = setupWidthTestTextLayout();

    final int resolvedWidth =
        TextSpec.resolveWidth(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            layout,
            false /* minimallyWide */,
            0 /* minimallyWideThreshold */);

    assertEquals(resolvedWidth, FULL_TEXT_WIDTH);
  }

  @Test
  public void testMinimallyWideText() {
    final Layout layout = setupWidthTestTextLayout();

    final int resolvedWidth =
        TextSpec.resolveWidth(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            layout,
            true /* minimallyWide */,
            FULL_TEXT_WIDTH - MINIMAL_TEXT_WIDTH - 1 /* minimallyWideThreshold */);

    assertEquals(resolvedWidth, MINIMAL_TEXT_WIDTH);
  }

  @Test
  public void testMinimallyWideThresholdText() {
    final Layout layout = setupWidthTestTextLayout();

    final int resolvedWidth =
        TextSpec.resolveWidth(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            layout,
            true /* minimallyWide */,
            FULL_TEXT_WIDTH - MINIMAL_TEXT_WIDTH /* minimallyWideThreshold */);

    assertEquals(resolvedWidth, FULL_TEXT_WIDTH);
  }

  private static Layout setupWidthTestTextLayout() {
    final Layout layout = mock(Layout.class);
    final int fullWidth = FULL_TEXT_WIDTH;
    final int minimalWidth = MINIMAL_TEXT_WIDTH;

    when(layout.getLineCount()).thenReturn(2);
    when(layout.getWidth()).thenReturn(fullWidth);
    when(layout.getLineRight(anyInt())).thenReturn((float) minimalWidth);

    return layout;
  }
}
