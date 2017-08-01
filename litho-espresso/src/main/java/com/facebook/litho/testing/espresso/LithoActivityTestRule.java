/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.espresso;

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;
import com.facebook.litho.config.ComponentsConfiguration;

/**
 * A test rule like {@link ActivityTestRule} ensuring that the activity is launched in
 * end to end test mode.
 *
 * Use in your Espresso tests like this:
 *
 * <pre>{@code  @Rule
 * public LithoActivityTestRule<DemoListActivity> mActivity =
 *     new LithoActivityTestRule<>(DemoListActivity.class);
 * }</pre>
 */
public class LithoActivityTestRule<T extends Activity> extends ActivityTestRule<T> {
  public LithoActivityTestRule(Class<T> activityClass) {
    super(activityClass);
  }

  public LithoActivityTestRule(Class<T> activityClass, boolean initialTouchMode) {
    super(activityClass, initialTouchMode);
  }

  public LithoActivityTestRule(Class<T> activityClass, boolean initialTouchMode,
                               boolean launchActivity) {
    super(activityClass, initialTouchMode, launchActivity);
  }

  @Override
  protected void beforeActivityLaunched() {
    super.beforeActivityLaunched();
    ComponentsConfiguration.isEndToEndTestRun = true;
  }
}
