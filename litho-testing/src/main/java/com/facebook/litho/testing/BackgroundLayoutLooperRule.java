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

package com.facebook.litho.testing;

import com.facebook.litho.config.ComponentsConfiguration;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * TestRule which allows a test to manually step through the default ComponentTree layout thread
 * Looper while still executing those tasks on a background thread. Normal usage of ShadowLooper
 * will execute on the calling thread, which in tests will execute code on the main thread.
 */
public class BackgroundLayoutLooperRule implements TestRule {

  BaseThreadLooperController threadLooperController = new ThreadLooperController();

  private void ensureThreadLooperType() {
    if (ComponentsConfiguration.isSplitResolveAndLayoutWithSplitHandlers()
        && threadLooperController instanceof ThreadLooperController) {
      threadLooperController = new ResolveAndLayoutThreadLooperController();
    } else if (!ComponentsConfiguration.isSplitResolveAndLayoutWithSplitHandlers()
        && threadLooperController instanceof ResolveAndLayoutThreadLooperController) {
      threadLooperController = new ThreadLooperController();
    }
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        ensureThreadLooperType();
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
    ensureThreadLooperType();
    threadLooperController.runOneTaskSync();
  }

  /** Runs through all tasks on the background thread, blocking until it completes. */
  public void runToEndOfTasksSync() {
    ensureThreadLooperType();
    threadLooperController.runToEndOfTasksSync();
  }

  public TimeOutSemaphore runToEndOfTasksAsync() {
    ensureThreadLooperType();
    return threadLooperController.runToEndOfTasksAsync();
  }
}
