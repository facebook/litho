/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.View;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link Text} component.
 */

@RunWith(ComponentsTestRunner.class)
public class TextSpecTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
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
    MountableCharSequence mountableCharSequence = mock(MountableCharSequence.class);

    TextDrawable drawable = getMountedDrawableForText(mountableCharSequence);
    verify(mountableCharSequence).onMount(drawable);
  }

  @Test
  public void testColorDefault() {
    TextDrawable drawable = getMountedDrawableForText("Some text");
    assertThat(drawable.getColor() == Color.BLACK);
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
    assertThat(drawable.getColor() == Color.RED);
  }

  @Test
  public void testColor()
  {
    TextDrawable drawable = getMountedDrawableForTextWithColors(
        "Some text",
        Color. RED,
        null);
    assertThat(drawable.getColor() == Color.RED);
  }

  @Test
  public void testColorStateList()
  {
    int[][] states = {{0}};
    int[] colors = {Color.GREEN};
    ColorStateList colorStateList = new ColorStateList(states, colors);
    TextDrawable drawable = getMountedDrawableForTextWithColors(
        "Some text",
        0,
        colorStateList);
    assertThat(drawable.getColor() == Color.RED);
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
