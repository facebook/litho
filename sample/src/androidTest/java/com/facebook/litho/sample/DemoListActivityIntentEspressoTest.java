/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sample;

import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.facebook.samples.litho.DemoActivity;
import com.facebook.samples.litho.DemoListActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.BundleMatchers.hasEntry;
import static android.support.test.espresso.intent.matcher.ComponentNameMatchers.hasClassName;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static android.support.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.componentHostWithText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DemoListActivityIntentEspressoTest {
  @Rule
  public IntentsTestRule<DemoListActivity> mActivity =
      new IntentsTestRule<>(DemoListActivity.class);

  @Test
  public void testPlaygroundLaunches() {
    onView(componentHostWithText(containsString("Playground")))
        .check(matches(isDisplayed()))
        .perform(click());

    intended(allOf(
      toPackage("com.facebook.samples.litho"),
      hasComponent(hasClassName(DemoActivity.class.getName())),
      hasExtras(hasEntry(equalTo("demoName"), equalTo("Playground")))));
  }
}
