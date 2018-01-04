/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.espresso.rules;

import android.support.test.InstrumentationRegistry;
import com.facebook.litho.dataflow.ChoreographerCompatImpl;
import com.facebook.litho.dataflow.DataFlowGraph;
import com.facebook.litho.dataflow.MockTimingSource;
import com.facebook.litho.dataflow.springs.SpringConfig;
import org.junit.rules.ExternalResource;

/**
 * A test rule for instrumentation and screenshot tests that need control over animation driving.
 */
public class AnimationRunnerTestRule extends ExternalResource {

  private MockTimingSource mFakeTimingSource;

  @Override
  protected void before() throws Throwable {
    mFakeTimingSource = new MockTimingSource();
    mFakeTimingSource.start();
    DataFlowGraph.setInstance(DataFlowGraph.create(mFakeTimingSource));
    SpringConfig.defaultConfig = new SpringConfig(20, 10);
    ChoreographerCompatImpl.setInstance(mFakeTimingSource);
  }

  /** Skip the given amount of frames */
  public void stepAnimationFrames(final int numFrames) {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            new Runnable() {
              @Override
              public void run() {
                mFakeTimingSource.step(numFrames);
              }
            });

    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }
}
