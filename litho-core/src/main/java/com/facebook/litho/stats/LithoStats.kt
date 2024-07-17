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

package com.facebook.litho.stats

import androidx.annotation.VisibleForTesting
import java.util.concurrent.atomic.AtomicLong

/** Provides global counters about Litho internals. Useful for performance analyses. */
object LithoStats {
  private val _componentAppliedStateUpdateCount = AtomicLong(0)
  private val _componentTriggeredSyncStateUpdateCount = AtomicLong(0)
  private val _componentTriggeredAsyncStateUpdateCount = AtomicLong(0)
  private val _componentCalculateLayoutCount = AtomicLong(0)
  private val _componentCalculateLayoutOnUICount = AtomicLong(0)
  private val _componentMountCount = AtomicLong(0)
  private val _resolveCount = AtomicLong(0)
  private val _resumeCount = AtomicLong(0)
  private val _layoutCount = AtomicLong(0)
  private val _sectionAppliedStateUpdateCount = AtomicLong(0)
  private val _sectionTriggeredSyncStateUpdateCount = AtomicLong(0)
  private val _sectionTriggeredAsyncStateUpdateCount = AtomicLong(0)
  private val _sectionCalculateNewChangesetCount = AtomicLong(0)
  private val _sectionCalculateNewChangesetOnUICount = AtomicLong(0)
  private val _resolveCancelledCount = AtomicLong(0)
  private val _layoutCancelledCount = AtomicLong(0)

  @get:JvmStatic
  val componentAppliedStateUpdateCount: Long
    /**
     * @return the global count of all applied state updates (async, lazy and sync) in Litho
     *   components that have happened in the process.
     */
    get() = _componentAppliedStateUpdateCount.get()

  @get:JvmStatic
  val componentTriggeredSyncStateUpdateCount: Long
    /**
     * @return the global count of all triggered synchronous state updates in Litho components that
     *   have happened in the process.
     */
    get() = _componentTriggeredSyncStateUpdateCount.get()

  @get:JvmStatic
  val componentTriggeredAsyncStateUpdateCount: Long
    /**
     * @return the global count of all triggered asynchronous state updates in Litho components that
     *   have happened in the process.
     */
    get() = _componentTriggeredAsyncStateUpdateCount.get()

  @get:JvmStatic
  val componentCalculateLayoutCount: Long
    /**
     * @return the global count of all layout calculations in Litho components that have happened in
     *   the process.
     */
    get() = _componentCalculateLayoutCount.get()

  @get:JvmStatic
  val componentCalculateLayoutOnUICount: Long
    /**
     * @return the global count of all UI Thread executed layout calculations in Litho components
     *   that have happened in the process.
     */
    get() = _componentCalculateLayoutOnUICount.get()

  @get:JvmStatic
  val componentMountCount: Long
    /** @return the global count of all mount operations that have happened in the process. */
    get() = _componentMountCount.get()

  @get:JvmStatic
  val resolveCount: Long
    /** @return the global count of all do resolve operations that have happened in the process. */
    get() = _resolveCount.get()

  @get:JvmStatic
  val resumeCount: Long
    /** @return the global count of all do resume operations that have happened in the process. */
    get() = _resumeCount.get()

  @get:JvmStatic
  val layoutCount: Long
    /** @return the global count of all do layout operations that have happened in the process. */
    get() = _layoutCount.get()

  @get:JvmStatic
  val resolveCancelledCount: Long
    /** @return the global count of all do resolve operations that have been avoided/cancelled. */
    get() = _resolveCancelledCount.get()

  @get:JvmStatic
  val layoutCancelledCount: Long
    /** @return the global count of all do layout operations that have been avoided/cancelled. */
    get() = _layoutCancelledCount.get()

  @get:JvmStatic
  val sectionAppliedStateUpdateCount: Long
    /**
     * @return the global count of all applied state updates (async, lazy and sync) in Litho
     *   sections that have happened in the process.
     */
    get() = _sectionAppliedStateUpdateCount.get()

  @get:JvmStatic
  val sectionTriggeredSyncStateUpdateCount: Long
    /**
     * @return the global count of all triggered synchronous state updates in Litho sections that
     *   have happened in the process.
     */
    get() = _sectionTriggeredSyncStateUpdateCount.get()

  @get:JvmStatic
  val sectionTriggeredAsyncStateUpdateCount: Long
    /**
     * @return the global count of all triggered asynchronous state updates in Litho sections that
     *   have happened in the process.
     */
    get() = _sectionTriggeredAsyncStateUpdateCount.get()

  @get:JvmStatic
  val sectionCalculateNewChangesetCount: Long
    /**
     * @return the global count of all new changeset calculations in Litho sections that have
     *   happened in the process.
     */
    get() = _sectionCalculateNewChangesetCount.get()

  @get:JvmStatic
  val sectionCalculateNewChangesetOnUICount: Long
    /**
     * @return the global count of all UI Thread executed new changeset calculations in Litho
     *   sections that have happened in the process.
     */
    get() = _sectionCalculateNewChangesetOnUICount.get()

  /**
   * Increment the count of all applied state updates in Litho components by {@param num}.
   *
   * @return The new total number of all state updates in Litho components recorded.
   */
  @JvmStatic
  fun incrementComponentAppliedStateUpdateCountBy(num: Long): Long =
      _componentAppliedStateUpdateCount.addAndGet(num)

  /**
   * Increment the count of triggered synchronous state updates in Litho components (by one).
   *
   * @return The new total number of synchronous state updates in Litho components recorded.
   */
  @JvmStatic
  fun incrementComponentStateUpdateSyncCount(): Long =
      _componentTriggeredSyncStateUpdateCount.addAndGet(1)

  /**
   * Increment the count of triggered asynchronous state updates in Litho components (by one).
   *
   * @return The new total number of asynchronous state updates in Litho components recorded.
   */
  @JvmStatic
  fun incrementComponentStateUpdateAsyncCount(): Long =
      _componentTriggeredAsyncStateUpdateCount.addAndGet(1)

  @JvmStatic
  @VisibleForTesting
  fun resetComponentStateUpdateAsyncCount() {
    _componentTriggeredAsyncStateUpdateCount.set(0)
  }

  /**
   * Increment the count of layout calculations in Litho components (by one).
   *
   * @return The new total number of layout calculations recorded.
   */
  @JvmStatic
  fun incrementComponentCalculateLayoutCount(): Long = _componentCalculateLayoutCount.addAndGet(1)

  /**
   * Increment the count of UI Thread exectued layout calculations in Litho components (by one).
   *
   * @return The new total number of UI Thread executed layout calculations recorded.
   */
  @JvmStatic
  fun incrementComponentCalculateLayoutOnUICount(): Long =
      _componentCalculateLayoutOnUICount.addAndGet(1)

  /**
   * Increment the count of mount operations (by one).
   *
   * @return The new total number of mount operations recorded.
   */
  @JvmStatic fun incrementComponentMountCount(): Long = _componentMountCount.addAndGet(1)

  /**
   * @return increment and get the global count of all do resolve operations that have happened in
   *   the process.
   */
  @JvmStatic fun incrementResolveCount(): Long = _resolveCount.addAndGet(1)

  /**
   * @return increment and get the global count of all do resolve operations that have been
   *   cancelled in the process.
   */
  @JvmStatic fun incrementCancelledResolve(): Long = _resolveCancelledCount.addAndGet(1)

  /**
   * @return increment and get the global count of all do resumes operations that have happened in
   *   the process.
   */
  @JvmStatic fun incrementResumeCount(): Long = _resumeCount.addAndGet(1)

  /**
   * @return increment and get the global count of all do layout operations that have happened in
   *   the process.
   */
  @JvmStatic fun incrementLayoutCount(): Long = _layoutCount.addAndGet(1)

  /**
   * @return increment and get the global count of all do layout operations that have been cancelled
   *   in the process.
   */
  @JvmStatic fun incrementCancelledLayout(): Long = _layoutCancelledCount.addAndGet(1)

  /**
   * Increment the count of all applied state updates in Litho sections by {@param num}.
   *
   * @return The new total number of all state updates in Litho sections recorded.
   */
  @JvmStatic
  fun incrementSectionAppliedStateUpdateCountBy(num: Long): Long =
      _sectionAppliedStateUpdateCount.addAndGet(num)

  /**
   * Increment the count of triggered synchronous state updates in Litho sections (by one).
   *
   * @return The new total number of synchronous state updates in Litho sections recorded.
   */
  @JvmStatic
  fun incrementSectionStateUpdateSyncCount(): Long =
      _sectionTriggeredSyncStateUpdateCount.addAndGet(1)

  /**
   * Increment the count of triggered asynchronous state updates in Litho sections (by one).
   *
   * @return The new total number of asynchronous state updates in Litho sections recorded.
   */
  @JvmStatic
  fun incrementSectionStateUpdateAsyncCount(): Long =
      _sectionTriggeredAsyncStateUpdateCount.addAndGet(1)

  /**
   * Increment the count of new changeset calculations in Litho sections (by one).
   *
   * @return The new total number of new changeset calculations in Litho sections recorded.
   */
  @JvmStatic
  fun incrementSectionCalculateNewChangesetCount(): Long =
      _sectionCalculateNewChangesetCount.addAndGet(1)

  /**
   * Increment the count of UI Thread executed new changeset calculations in Litho sections (by
   * one).
   *
   * @return The new total number of UI Thread executed new changeset calculations in Litho sections
   *   recorded.
   */
  @JvmStatic
  fun incrementSectionCalculateNewChangesetOnUICount(): Long =
      _sectionCalculateNewChangesetOnUICount.addAndGet(1)

  @JvmStatic
  @VisibleForTesting
  @Synchronized
  fun resetAllCounters() {
    _componentAppliedStateUpdateCount.set(0)
    _componentTriggeredSyncStateUpdateCount.set(0)
    _componentTriggeredAsyncStateUpdateCount.set(0)
    _componentCalculateLayoutCount.set(0)
    _componentCalculateLayoutOnUICount.set(0)
    _componentMountCount.set(0)
    _layoutCount.set(0)
    _layoutCancelledCount.set(0)
    _resolveCount.set(0)
    _resolveCancelledCount.set(0)
    _resumeCount.set(0)
    _sectionAppliedStateUpdateCount.set(0)
    _sectionTriggeredSyncStateUpdateCount.set(0)
    _sectionTriggeredAsyncStateUpdateCount.set(0)
    _sectionCalculateNewChangesetCount.set(0)
    _sectionCalculateNewChangesetOnUICount.set(0)
  }
}
