/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sample;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.facebook.samples.litho.DemoListActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.componentHostWithText;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DemoListActivityEspressoTest {
  @Rule
  public ActivityTestRule<DemoListActivity> mActivity =
      new ActivityTestRule<>(DemoListActivity.class);

  @Test
  public void testLithographyIsVisibleAndClickable() {
    onView(componentHostWithText(containsString("Lithography")))
        .check(matches(isDisplayed()))
        .check(matches(isClickable()));
  }

  @Test
  public void testPlaygroundIsVisibleAndClickable() {
    onView(componentHostWithText(containsString("Playground")))
        .check(matches(isDisplayed()))
        .check(matches(isClickable()));
  }
}
