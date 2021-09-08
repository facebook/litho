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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * TestRule which allows a test to manually step through the default ComponentTree layout thread
 * Looper while still executing those tasks on a background thread. Normal usage of ShadowLooper
 * will execute on the calling thread, which in tests will execute code on the main thread.
 */
public class BackgroundLayoutLooperRule implements TestRule {

  ThreadLooperController threadLooperController = new ThreadLooperController();

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        threadLooperController.init();
        try {
          base.evaluate();
        } finally {
          threadLooperController.clean();
        }
      }
    };
  }

  /** Runs one task on the background thread, blocking until it completes. */
  public void runOneTaskSync() {
    threadLooperController.runOneTaskSync();
  }

  /** Runs through all tasks on the background thread, blocking until it completes. */
  public void runToEndOfTasksSync() {
    threadLooperController.runToEndOfTasksSync();
  }

  public TimeOutSemaphore runToEndOfTasksAsync() {
    return threadLooperController.runToEndOfTasksAsync();
  }
}
