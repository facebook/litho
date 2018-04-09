/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import com.facebook.litho.config.ComponentsConfiguration;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * A test rule to be able to turn on/off Litho animations with different granularity, i.e. at
 * class-level, test method level or at code block level.
 */
public class LithoAnimationEnablerTestRule extends TestWatcher {

  private final boolean mOriginalValue =
      ComponentsConfiguration.forceEnableTransitionsForInstrumentationTests;

  /**
   * Temporarily enable Litho animations until either {@link #disable()} is called or test method
   * finishes. If you want to enable animations at class level you can use this method in setup
   * method (annotated with @Before), or in individual test methods at the beginning of the method.
   * We will make sure to turn animation back to its original value when every test method finishes.
   */
  public void enable() {
    ComponentsConfiguration.forceEnableTransitionsForInstrumentationTests = true;
  }

  /**
   * Disable Litho animations. This might be useful for cases where within a test method you can
   * enable animations for certain actions, while disabling it for others all within single test
   * method. Note that there is no need to disable on tear down of the test class as we make sure to
   * turn animation back to its original value when every test method finishes.
   */
  public void disable() {
    ComponentsConfiguration.forceEnableTransitionsForInstrumentationTests = false;
  }

  @Override
  protected void finished(Description description) {
    ComponentsConfiguration.forceEnableTransitionsForInstrumentationTests = mOriginalValue;
  }
}
