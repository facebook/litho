/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import androidx.annotation.Nullable;
import java.util.Map;
import javax.annotation.CheckReturnValue;

/**
 * This class provides utilities for extracting information through {@link
 * ComponentsLogger#getExtraAnnotations(TreeProps)} and transforming them so they can be logged.
 */
public final class LogTreePopulator {
  private LogTreePopulator() {}

  /**
   * Annotate a log event with the log tag set in the context, and extract the treeprops from a
   * given {@link ComponentContext} and convert them into perf event annotations using a {@link
   * ComponentsLogger} implementation.
   *
   * @return Annotated perf event, or <code>null</code> if the resulting event isn't deemed worthy
   *     of reporting.
   */
  @Nullable
  @CheckReturnValue
  public static PerfEvent populatePerfEventFromLogger(
      ComponentContext c, ComponentsLogger logger, @Nullable PerfEvent perfEvent) {
    return populatePerfEventFromLogger(c, c.getTreeProps(), logger, perfEvent);
  }

  /**
   * Annotate a log event with the log tag set in the context, and extract the treeprops from a
   * given {@link ComponentContext} or saved treeprops and convert them into perf event annotations
   * using a {@link ComponentsLogger} implementation. If the treeprops of the given
   * {@link ComponentContext} is null, the saved treeprops will be used.
   *
   * @return Annotated perf event, or <code>null</code> if the resulting event isn't deemed worthy
   *     of reporting.
   */
  @Nullable
  @CheckReturnValue
  public static PerfEvent populatePerfEventFromLogger(
          ComponentContext c, @Nullable TreeProps savedTreeProps, ComponentsLogger logger, @Nullable PerfEvent perfEvent) {
    if (perfEvent == null) {
      return null;
    }
    final String logTag = c.getLogTag();
    if (logTag == null) {
      logger.cancelPerfEvent(perfEvent);
      return null;
    }

    perfEvent.markerAnnotate(FrameworkLogEvents.PARAM_LOG_TAG, logTag);

    TreeProps treeProps = c.getTreeProps() == null ? savedTreeProps : c.getTreeProps();
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
   * Turn the extracted tree props from a {@link ComponentsLogger} and turn them into a single
   * colon-separated string that
   *
   * @see #populatePerfEventFromLogger(ComponentContext, ComponentsLogger, PerfEvent)
   * @param component Component to extract tree props from.
   * @param logger
   * @return String of extracted props with key-value pairs separated by ':'.
   */
  @Nullable
  public static String getAnnotationBundleFromLogger(Component component, ComponentsLogger logger) {
    @Nullable final ComponentContext scopedContext = component.getScopedContext();
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
      sb.append(':');
    }

    return sb.toString();
  }
}
