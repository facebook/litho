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
import android.support.test.runner.AndroidJUnit4;
import android.test.UiThreadTest;
import android.support.test.rule.UiThreadTestRule;
import android.widget.TextView;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.Text;
import com.facebook.testing.screenshot.ViewHelpers;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link ComponentHostMatchers}
 */
@RunWith(AndroidJUnit4.class)
public class ComponentHostMatchersTest {

  @Rule
  public UiThreadTestRule mUiThreadRule = new UiThreadTestRule();

  private LithoView mView;

  @Before
  public void before() throws Throwable {
    final ComponentContext mComponentContext = new ComponentContext(
        InstrumentationRegistry.getTargetContext());
    final Component mTextComponent = MyComponent.create(mComponentContext)
        .text("foobar")
        .customViewTag("zoidberg")
        .build();
    final ComponentTree tree = ComponentTree.create(
        mComponentContext,
        mTextComponent)
        .build();
    mView = new LithoView(mComponentContext);
    mView.setComponentTree(tree);
    ViewHelpers.setupView(mView)
        .setExactWidthPx(200)
        .setExactHeightPx(100)
        .layout();
  }

  @Test
  public void testContentDescriptionMatching() throws Throwable {

    assertThat(
        mView,
        componentHostWithText("foobar"));
    assertThat(
        mView,
        not(componentHostWithText("bar")));
    assertThat(
        mView,
        componentHostWithText(containsString("oob")));
  }

  @Test
  public void testIsComponentHost() throws Throwable {
    assertThat(
        new TextView(InstrumentationRegistry.getTargetContext()),
        is(not(componentHost())));
    assertThat(
        mView,
        is(componentHost()));
  }

  @Test
  public void testIsComponentHostWithMatcher() throws Throwable {
    assertThat(
        mView,
        is(componentHost(withText("foobar"))));
    assertThat(
        mView,
        is(not(componentHost(withText("blah")))));
  }

  @Test
  public void testContentDescription() throws Throwable {
    assertThat(
        mView,
        is(componentHost(withContentDescription("foobar2"))));
  }

  @Test
  public void testMountedComponent() throws Throwable {
    assertThat(
        mView,
        is(componentHost(withLifecycle(isA(Text.class)))));
  }
}
