// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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

import com.facebook.litho.stats.LithoStats;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This rule resets the litho stats counters {@link LithoStats#resetAllCounters} after every test,
 * and provides utilities methods to get the litho stats.
 */
public class LithoStatsRule implements TestRule {

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          LithoStats.resetAllCounters();
          base.evaluate();
        } finally {
          LithoStats.resetAllCounters();
        }
      }
    };
  }

  /**
   * @return the global count of all applied state updates (async, lazy and sync) in Litho
   *     components that have happened in the process.
   */
  public long getComponentAppliedStateUpdateCount() {
    return LithoStats.getComponentAppliedStateUpdateCount();
  }

  /**
   * @return the global count of all triggered synchronous state updates in Litho components that
   *     have happened in the process.
   */
  public long getComponentTriggeredSyncStateUpdateCount() {
    return LithoStats.getComponentTriggeredSyncStateUpdateCount();
  }

  /**
   * @return the global count of all triggered asynchronous state updates in Litho components that
   *     have happened in the process.
   */
  public long getComponentTriggeredAsyncStateUpdateCount() {
    return LithoStats.getComponentTriggeredAsyncStateUpdateCount();
  }

  /**
   * @return the global count of all layout calculations in Litho components that have happened in
   *     the process.
   */
  public long getComponentCalculateLayoutCount() {
    return LithoStats.getComponentCalculateLayoutCount();
  }

  /**
   * @return the global count of all UI Thread executed layout calculations in Litho components that
   *     have happened in the process.
   */
  public long getComponentCalculateLayoutOnUICount() {
    return LithoStats.getComponentCalculateLayoutOnUICount();
  }

  /** @return the global count of all mount operations that have happened in the process. */
  public long getComponentMountCount() {
    return LithoStats.getComponentMountCount();
  }

  /**
   * @return the global count of all applied state updates (async, lazy and sync) in Litho sections
   *     that have happened in the process.
   */
  public long getSectionAppliedStateUpdateCount() {
    return LithoStats.getSectionAppliedStateUpdateCount();
  }

  /**
   * @return the global count of all triggered synchronous state updates in Litho sections that have
   *     happened in the process.
   */
  public long getSectionTriggeredSyncStateUpdateCount() {
    return LithoStats.getSectionTriggeredSyncStateUpdateCount();
  }

  /**
   * @return the global count of all triggered asynchronous state updates in Litho sections that
   *     have happened in the process.
   */
  public long getSectionTriggeredAsyncStateUpdateCount() {
    return LithoStats.getSectionTriggeredAsyncStateUpdateCount();
  }

  /**
   * @return the global count of all new changeset calculations in Litho sections that have happened
   *     in the process.
   */
  public long getSectionCalculateNewChangesetCount() {
    return LithoStats.getSectionCalculateNewChangesetCount();
  }

  /**
   * @return the global count of all UI Thread executed new changeset calculations in Litho sections
   *     that have happened in the process.
   */
  public long getSectionCalculateNewChangesetOnUICount() {
    return LithoStats.getSectionCalculateNewChangesetOnUICount();
  }

  /** Resets all the counter. */
  public synchronized void resetAllCounters() {
    LithoStats.resetAllCounters();
  }
}
