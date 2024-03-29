/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.sections.tti;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.componentHostWithText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;

import androidx.test.runner.AndroidJUnit4;
import com.facebook.litho.testing.espresso.LithoActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TTIMarkerTest {
  @Rule
  public LithoActivityTestRule<TTIMarkerActivity> mActivity =
      new LithoActivityTestRule<>(TTIMarkerActivity.class);

  @Test
  public void testRenderCompleteEventTriggered() {
    onView(componentHostWithText(is("Hello World"))).check(matches(isDisplayed()));
    assertToastIsShown(TTIMarkerSectionSpec.RENDER_MARKER);
  }

  private void assertToastIsShown(String text) {
    onView(withText(text))
        .inRoot(withDecorView(not(is(mActivity.getActivity().getWindow().getDecorView()))))
        .check(matches(isDisplayed()));
  }
}
