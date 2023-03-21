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

package com.facebook.litho

import javax.annotation.CheckReturnValue

/**
 * This class provides utilities for extracting information through
 * [ComponentsLogger#getExtraAnnotations(TreeProps)] and transforming them so they can be logged.
 */
object LogTreePopulator {
  /**
   * Annotate a log event with the log tag set in the context, and extract the [TreeProps] from a
   * given [ComponentContext] and convert them into perf event annotations using a
   * [ComponentsLogger] implementation.
   *
   * @return Annotated perf event, or `null` if the resulting event isn't deemed worthy of
   *   reporting.
   */
  @CheckReturnValue
  @JvmStatic
  fun populatePerfEventFromLogger(
      c: ComponentContext,
      logger: ComponentsLogger,
      perfEvent: PerfEvent?
  ): PerfEvent? {
    return populatePerfEventFromLogger(logger, c.logTag, perfEvent, c.treeProps)
  }

  /**
   * Annotate a log event with the log tag set in the context, and extract the [TreeProps] from a
   * given [ComponentContext] and convert them into perf event annotations using a
   * [ComponentsLogger] implementation.
   *
   * @return Annotated perf event, or `null` if the resulting event isn't deemed worthy of
   *   reporting.
   */
  @CheckReturnValue
  @JvmStatic
  fun populatePerfEventFromLogger(
      c: ComponentContext,
      logger: ComponentsLogger,
      logTag: String?,
      perfEvent: PerfEvent?
  ): PerfEvent? {
    return populatePerfEventFromLogger(logger, logTag, perfEvent, c.treeProps)
  }

  /**
   * Annotate a log event with the log tag set in the context, and convert the [TreeProps] into perf
   * event annotations using a [ComponentsLogger] implementation.
   *
   * @return Annotated perf event, or `null` if the resulting event isn't deemed worthy of
   *   reporting.
   */
  @CheckReturnValue
  private fun populatePerfEventFromLogger(
      logger: ComponentsLogger,
      logTag: String?,
      perfEvent: PerfEvent?,
      treeProps: TreeProps?
  ): PerfEvent? {
    if (perfEvent == null) {
      return null
    }
    if (logTag == null) {
      logger.cancelPerfEvent(perfEvent)
      return null
    }
    perfEvent.markerAnnotate(FrameworkLogEvents.PARAM_LOG_TAG, logTag)
    if (treeProps == null) {
      return perfEvent
    }

    val extraAnnotations = logger.getExtraAnnotations { key -> treeProps[key] } ?: return perfEvent
    for ((key, value) in extraAnnotations) {
      perfEvent.markerAnnotate(key, value)
    }
    return perfEvent
  }
}
