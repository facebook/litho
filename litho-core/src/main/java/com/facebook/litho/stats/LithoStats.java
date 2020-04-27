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

package com.facebook.litho.stats;

import androidx.annotation.VisibleForTesting;
import java.util.concurrent.atomic.AtomicLong;

/** Provides global counters about Litho internals. Useful for performance analyses. */
public final class LithoStats {
  private static final AtomicLong sComponentAppliedStateUpdateCount = new AtomicLong(0);
  private static final AtomicLong sComponentTriggeredSyncStateUpdateCount = new AtomicLong(0);
  private static final AtomicLong sComponentTriggeredAsyncStateUpdateCount = new AtomicLong(0);
  private static final AtomicLong sComponentCalculateLayoutCount = new AtomicLong(0);
  private static final AtomicLong sComponentCalculateLayoutOnUICount = new AtomicLong(0);
  private static final AtomicLong sComponentMountCount = new AtomicLong(0);

  private static final AtomicLong sSectionAppliedStateUpdateCount = new AtomicLong(0);
  private static final AtomicLong sSectionTriggeredSyncStateUpdateCount = new AtomicLong(0);
  private static final AtomicLong sSectionTriggeredAsyncStateUpdateCount = new AtomicLong(0);
  private static final AtomicLong sSectionCalculateNewChangesetCount = new AtomicLong(0);
  private static final AtomicLong sSectionCalculateNewChangesetOnUICount = new AtomicLong(0);

  /**
   * @return the global count of all applied state updates (async, lazy and sync) in Litho
   *     components that have happened in the process.
   */
  public static long getComponentAppliedStateUpdateCount() {
    return sComponentAppliedStateUpdateCount.get();
  }

  /**
   * @return the global count of all triggered synchronous state updates in Litho components that
   *     have happened in the process.
   */
  public static long getComponentTriggeredSyncStateUpdateCount() {
    return sComponentTriggeredSyncStateUpdateCount.get();
  }

  /**
   * @return the global count of all triggered asynchronous state updates in Litho components that
   *     have happened in the process.
   */
  public static long getComponentTriggeredAsyncStateUpdateCount() {
    return sComponentTriggeredAsyncStateUpdateCount.get();
  }

  /**
   * @return the global count of all layout calculations in Litho components that have happened in
   *     the process.
   */
  public static long getComponentCalculateLayoutCount() {
    return sComponentCalculateLayoutCount.get();
  }

  /**
   * @return the global count of all UI Thread executed layout calculations in Litho components that
   *     have happened in the process.
   */
  public static long getComponentCalculateLayoutOnUICount() {
    return sComponentCalculateLayoutOnUICount.get();
  }

  /** @return the global count of all mount operations that have happened in the process. */
  public static long getComponentMountCount() {
    return sComponentMountCount.get();
  }

  /**
   * @return the global count of all applied state updates (async, lazy and sync) in Litho sections
   *     that have happened in the process.
   */
  public static long getSectionAppliedStateUpdateCount() {
    return sSectionAppliedStateUpdateCount.get();
  }

  /**
   * @return the global count of all triggered synchronous state updates in Litho sections that have
   *     happened in the process.
   */
  public static long getSectionTriggeredSyncStateUpdateCount() {
    return sSectionTriggeredSyncStateUpdateCount.get();
  }

  /**
   * @return the global count of all triggered asynchronous state updates in Litho sections that
   *     have happened in the process.
   */
  public static long getSectionTriggeredAsyncStateUpdateCount() {
    return sSectionTriggeredAsyncStateUpdateCount.get();
  }

  /**
   * @return the global count of all new changeset calculations in Litho sections that have happened
   *     in the process.
   */
  public static long getSectionCalculateNewChangesetCount() {
    return sSectionCalculateNewChangesetCount.get();
  }

  /**
   * @return the global count of all UI Thread executed new changeset calculations in Litho sections
   *     that have happened in the process.
   */
  public static long getSectionCalculateNewChangesetOnUICount() {
    return sSectionCalculateNewChangesetOnUICount.get();
  }

  /**
   * Increment the count of all applied state updates in Litho components by {@param num}.
   *
   * @return The new total number of all state updates in Litho components recorded.
   */
  public static long incrementComponentAppliedStateUpdateCountBy(final long num) {
    return sComponentAppliedStateUpdateCount.addAndGet(num);
  }

  /**
   * Increment the count of triggered synchronous state updates in Litho components (by one).
   *
   * @return The new total number of synchronous state updates in Litho components recorded.
   */
  public static long incrementComponentStateUpdateSyncCount() {
    return sComponentTriggeredSyncStateUpdateCount.addAndGet(1);
  }

  /**
   * Increment the count of triggered asynchronous state updates in Litho components (by one).
   *
   * @return The new total number of asynchronous state updates in Litho components recorded.
   */
  public static long incrementComponentStateUpdateAsyncCount() {
    return sComponentTriggeredAsyncStateUpdateCount.addAndGet(1);
  }

  @VisibleForTesting
  public static void resetComponentStateUpdateAsyncCount() {
    sComponentTriggeredAsyncStateUpdateCount.set(0);
  }

  /**
   * Increment the count of layout calculations in Litho components (by one).
   *
   * @return The new total number of layout calculations recorded.
   */
  public static long incrementComponentCalculateLayoutCount() {
    return sComponentCalculateLayoutCount.addAndGet(1);
  }

  /**
   * Increment the count of UI Thread exectued layout calculations in Litho components (by one).
   *
   * @return The new total number of UI Thread executed layout calculations recorded.
   */
  public static long incrementComponentCalculateLayoutOnUICount() {
    return sComponentCalculateLayoutOnUICount.addAndGet(1);
  }

  /**
   * Increment the count of mount operations (by one).
   *
   * @return The new total number of mount operations recorded.
   */
  public static long incrementComponentMountCount() {
    return sComponentMountCount.addAndGet(1);
  }

  /**
   * Increment the count of all applied state updates in Litho sections by {@param num}.
   *
   * @return The new total number of all state updates in Litho sections recorded.
   */
  public static long incrementSectionAppliedStateUpdateCountBy(final long num) {
    return sSectionAppliedStateUpdateCount.addAndGet(num);
  }

  /**
   * Increment the count of triggered synchronous state updates in Litho sections (by one).
   *
   * @return The new total number of synchronous state updates in Litho sections recorded.
   */
  public static long incrementSectionStateUpdateSyncCount() {
    return sSectionTriggeredSyncStateUpdateCount.addAndGet(1);
  }

  /**
   * Increment the count of triggered asynchronous state updates in Litho sections (by one).
   *
   * @return The new total number of asynchronous state updates in Litho sections recorded.
   */
  public static long incrementSectionStateUpdateAsyncCount() {
    return sSectionTriggeredAsyncStateUpdateCount.addAndGet(1);
  }

  /**
   * Increment the count of new changeset calculations in Litho sections (by one).
   *
   * @return The new total number of new changeset calculations in Litho sections recorded.
   */
  public static long incrementSectionCalculateNewChangesetCount() {
    return sSectionCalculateNewChangesetCount.addAndGet(1);
  }

  /**
   * Increment the count of UI Thread executed new changeset calculations in Litho sections (by
   * one).
   *
   * @return The new total number of UI Thread executed new changeset calculations in Litho sections
   *     recorded.
   */
  public static long incrementSectionCalculateNewChangesetOnUICount() {
    return sSectionCalculateNewChangesetOnUICount.addAndGet(1);
  }

  @VisibleForTesting
  public static synchronized void resetAllCounters() {
    sComponentAppliedStateUpdateCount.set(0);
    sComponentTriggeredSyncStateUpdateCount.set(0);
    sComponentTriggeredAsyncStateUpdateCount.set(0);
    sComponentCalculateLayoutCount.set(0);
    sComponentCalculateLayoutOnUICount.set(0);
    sComponentMountCount.set(0);
    sSectionAppliedStateUpdateCount.set(0);
    sSectionTriggeredSyncStateUpdateCount.set(0);
    sSectionTriggeredAsyncStateUpdateCount.set(0);
    sSectionCalculateNewChangesetCount.set(0);
    sSectionCalculateNewChangesetOnUICount.set(0);
  }
}
