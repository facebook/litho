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

package com.facebook.litho.testing.espresso.rules;

import androidx.test.InstrumentationRegistry;
import com.facebook.litho.choreographercompat.ChoreographerCompatImpl;
import com.facebook.litho.dataflow.DataFlowGraph;
import com.facebook.litho.dataflow.MockTimingSource;
import com.facebook.litho.dataflow.springs.SpringConfig;
import org.junit.rules.ExternalResource;

/**
 * A test rule for instrumentation and screenshot tests that need control over animation driving.
 */
public class AnimationRunnerTestRule extends ExternalResource {

  public static final int FRAME_TIME_MS = MockTimingSource.FRAME_TIME_MS;

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
