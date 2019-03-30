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

package com.facebook.litho.testing.espresso;

import static com.facebook.litho.testing.espresso.LithoViewMatchers.lithoView;
import static com.facebook.litho.testing.espresso.LithoViewMatchers.withTestKey;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import android.widget.TextView;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.UiThreadTestRule;
import androidx.test.runner.AndroidJUnit4;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.testing.screenshot.ViewHelpers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    final Component mTextComponent = MyComponent.create(mComponentContext)
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
