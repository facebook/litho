/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.espresso;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.TextView;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.testing.screenshot.ViewHelpers;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.facebook.litho.testing.espresso.LithoViewMatchers.lithoView;
import static com.facebook.litho.testing.espresso.LithoViewMatchers.withTestKey;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link LithoViewMatchers}
 */
@RunWith(AndroidJUnit4.class)
public class LithoViewMatchersTest {

  @Rule
  public UiThreadTestRule mUiThreadRule = new UiThreadTestRule();

  private LithoView mView;

  @Before
  public void before() throws Throwable {
    mUiThreadRule.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        ComponentsConfiguration.isEndToEndTestRun = true;
      }
    });

    final ComponentContext mComponentContext = new ComponentContext(
        InstrumentationRegistry.getTargetContext());
    final Component<MyComponent> mTextComponent = MyComponent.create(mComponentContext)
        .text("foobar")
        .build();
    mView = LithoView.create(InstrumentationRegistry.getTargetContext(), mTextComponent);
    ViewHelpers.setupView(mView)
        .setExactWidthPx(200)
        .setExactHeightPx(100)
        .layout();
  }

  @Test
  public void testIsLithoView() throws Throwable {
    assertThat(
        new TextView(InstrumentationRegistry.getTargetContext()),
        is(not(lithoView())));
    assertThat(
        mView,
        is(lithoView()));
  }

  @Test
  public void testHasTestKey() throws Throwable {
    assertThat(
        mView,
        is(withTestKey("my_test_key")));
    assertThat(
        mView,
        is(not(withTestKey("my_non_existant_test_key"))));
  }
}
