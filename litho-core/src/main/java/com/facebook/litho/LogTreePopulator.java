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

package com.facebook.litho;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;
import javax.annotation.CheckReturnValue;

/**
 * This class provides utilities for extracting information through {@link
 * ComponentsLogger#getExtraAnnotations(TreeProps)} and transforming them so they can be logged.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class LogTreePopulator {
  private LogTreePopulator() {}

  /**
   * Annotate a log event with the log tag set in the context, and extract the {@link TreeProps}
   * from a given {@link ComponentContext} and convert them into perf event annotations using a
   * {@link ComponentsLogger} implementation.
   *
   * @return Annotated perf event, or <code>null</code> if the resulting event isn't deemed worthy
   *     of reporting.
   */
  @Nullable
  @CheckReturnValue
  public static PerfEvent populatePerfEventFromLogger(
      ComponentContext c, ComponentsLogger logger, @Nullable PerfEvent perfEvent) {
    return populatePerfEventFromLogger(logger, c.getLogTag(), perfEvent, c.getTreeProps());
  }

  /**
   * Annotate a log event with the log tag set in the context, and extract the {@link TreeProps}
   * from a given {@link ComponentContext} and convert them into perf event annotations using a
   * {@link ComponentsLogger} implementation.
   *
   * @return Annotated perf event, or <code>null</code> if the resulting event isn't deemed worthy
   *     of reporting.
   */
  @Nullable
  @CheckReturnValue
  public static PerfEvent populatePerfEventFromLogger(
      ComponentContext c,
      ComponentsLogger logger,
      @Nullable String logTag,
      @Nullable PerfEvent perfEvent) {
    return populatePerfEventFromLogger(logger, logTag, perfEvent, c.getTreeProps());
  }

  /**
   * Annotate a log event with the log tag set in the context, and convert the {@link TreeProps}
   * into perf event annotations using a {@link ComponentsLogger} implementation.
   *
   * @return Annotated perf event, or <code>null</code> if the resulting event isn't deemed worthy
   *     of reporting.
   */
  @Nullable
  @CheckReturnValue
  static PerfEvent populatePerfEventFromLogger(
      ComponentsLogger logger,
      @Nullable String logTag,
      @Nullable PerfEvent perfEvent,
      @Nullable TreeProps treeProps) {
    if (perfEvent == null) {
      return null;
    }

    if (logTag == null) {
      logger.cancelPerfEvent(perfEvent);
      return null;
    }

    perfEvent.markerAnnotate(FrameworkLogEvents.PARAM_LOG_TAG, logTag);

    if (treeProps == null) {
      return perfEvent;
    }

    @Nullable final Map<String, String> extraAnnotations = logger.getExtraAnnotations(treeProps);
    if (extraAnnotations == null) {
      return perfEvent;
    }

    for (Map.Entry<String, String> e : extraAnnotations.entrySet()) {
      perfEvent.markerAnnotate(e.getKey(), e.getValue());
    }

    return perfEvent;
  }

  /**
   * Extract treeprops from a scoped {@link ComponentContext} and turn them into a single
   * colon-separated string.
   *
   * @param scopedContext ComponentContext to extract tree props from.
   * @param logger ComponentsLogger to convert treeprops into perf event annotations.
   * @return String of extracted props with key-value pairs separated by ':'.
   * @see #populatePerfEventFromLogger(ComponentContext, ComponentsLogger, PerfEvent)
   */
  @Nullable
  public static String getAnnotationBundleFromLogger(
      ComponentContext scopedContext, ComponentsLogger logger) {
    if (scopedContext == null) {
      return null;
    }

    @Nullable final TreeProps treeProps = scopedContext.getTreeProps();
    if (treeProps == null) {
      return null;
    }

    @Nullable final Map<String, String> extraAnnotations = logger.getExtraAnnotations(treeProps);
    if (extraAnnotations == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder(extraAnnotations.size() * 16);
    for (Map.Entry<String, String> entry : extraAnnotations.entrySet()) {
      sb.append(entry.getKey());
      sb.append(':');
      sb.append(entry.getValue());
      sb.append(';');
    }

    return sb.toString();
  }
}
