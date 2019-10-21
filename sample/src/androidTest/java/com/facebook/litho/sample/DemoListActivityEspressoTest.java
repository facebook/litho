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

package com.facebook.litho.sample;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.componentHostWithText;
import static com.facebook.litho.testing.espresso.LithoViewMatchers.withTestKey;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;
import com.facebook.litho.testing.espresso.LithoActivityTestRule;
import com.facebook.samples.litho.DemoListActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DemoListActivityEspressoTest {
  @Rule
  public LithoActivityTestRule<DemoListActivity> mActivity =
      new LithoActivityTestRule<>(DemoListActivity.class);

  @Test
  public void testLithographyIsVisibleAndClickable() {
    onView(componentHostWithText(containsString("Lithography")))
        .check(matches(allOf(isDisplayed(), isClickable())));
  }

  @Test
  public void testTestKeyLookup() {
    onView(withTestKey("main_screen")).check(matches(isDisplayed()));
  }

  @Test
  public void testPlaygroundIsVisibleAndClickable() {
    onView(componentHostWithText(containsString("Playground")))
        .check(matches(isDisplayed()))
        .check(matches(isClickable()));
  }
}
