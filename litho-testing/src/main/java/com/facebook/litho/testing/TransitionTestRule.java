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

package com.facebook.litho.testing;

import com.facebook.litho.choreographercompat.ChoreographerCompatImpl;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.dataflow.DataFlowGraph;
import com.facebook.litho.dataflow.MockTimingSource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TransitionTestRule implements TestRule {

  private boolean mIsAnimationDisabled;
  private MockTimingSource mFakeTimingSource;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          mIsAnimationDisabled = ComponentsConfiguration.isAnimationDisabled;
          ComponentsConfiguration.isAnimationDisabled = false;
          mFakeTimingSource = new MockTimingSource();
          mFakeTimingSource.start();
          DataFlowGraph.setInstance(DataFlowGraph.create(mFakeTimingSource));
          ChoreographerCompatImpl.setInstance(mFakeTimingSource);
          base.evaluate();
        } finally {
          ComponentsConfiguration.isAnimationDisabled = mIsAnimationDisabled;
          ChoreographerCompatImpl.setInstance(null);
          DataFlowGraph.setInstance(null);
        }
      }
    };
  }

  public void step(int numFrames) {
    mFakeTimingSource.step(numFrames);
  }
}
