/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.sections.treeprops;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.componentHost;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.withTagValue;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AllOf.allOf;

import androidx.test.runner.AndroidJUnit4;
import com.facebook.litho.testing.espresso.LithoActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DiffSectionTreePropsTest {
  @Rule
  public LithoActivityTestRule<DiffingTreePropTestActivity> mActivity =
      new LithoActivityTestRule<>(DiffingTreePropTestActivity.class);

  @Test
  public void testTreePropPropagation() {
    onView(
            componentHost(
                allOf(withText(containsString("rootdiff:top:leaf")), withTagValue(is((Object) 0)))))
        .check(matches(isDisplayed()));
  }
}
