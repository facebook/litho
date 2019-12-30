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

import java.util.HashSet;
import javax.annotation.Nullable;

/**
 * Logger for tracking Litho events happening during startup.
 *
 * <p>The implementations decide how to log points (via {@link #onMarkPoint(String)}) and whether
 * this logger is enabled (via {@link #isEnabled()}.
 */
public abstract class LithoStartupLogger {
  public static final String LITHO_PREFIX = "litho";

  public static final String CHANGESET_CALCULATION = "_changeset";
  public static final String FIRST_LAYOUT = "_firstlayout";
  public static final String FIRST_MOUNT = "_firstmount";
  public static final String LAST_MOUNT = "_lastmount";

  public static final String START = "_start";
  public static final String END = "_end";

  private static final HashSet<String> NEEDS_THREAD_INFO = new HashSet<>();

  static {
    NEEDS_THREAD_INFO.add(CHANGESET_CALCULATION);
    NEEDS_THREAD_INFO.add(FIRST_LAYOUT);
  }

  public static boolean isEnabled(@Nullable LithoStartupLogger logger) {
    return logger != null && logger.isEnabled();
  }

  private String mDataAttribution = "";
  private final HashSet<String> mProcessedEvents = new HashSet<>();
  private final HashSet<String> mStartedEvents = new HashSet<>();

  private void markPoint(String name) {
    if (!mProcessedEvents.contains(name)) {
      onMarkPoint(name);
      mProcessedEvents.add(name);
    }
  }

  private String getFullMarkerName(String eventName, String dataAttribution, String stage) {
    final StringBuilder markerName = new StringBuilder();
    markerName.append(LithoStartupLogger.LITHO_PREFIX);
    if (NEEDS_THREAD_INFO.contains(eventName)) {
      markerName.append(ThreadUtils.isMainThread() ? "_ui" : "_bg");
    }
    if (!dataAttribution.isEmpty()) {
      markerName.append('_');
      markerName.append(dataAttribution);
    }
    markerName.append(eventName);
    markerName.append(stage);
    return markerName.toString();
  }

  /**
   * Set attribution to the rendered events like the network query name, data source (network/cache)
   * etc.
   */
  public void setDataAttribution(String attribution) {
    mDataAttribution = attribution;
  }

  /**
   * @return attribution to the rendered events like the network query name, data source
   *     (network/cache) etc.
   */
  public String getLatestDataAttribution() {
    return mDataAttribution;
  }

  /**
   * Mark the event with given name and stage (start/end). It will use currently assigned data
   * attribution.
   */
  public void markPoint(String eventName, String stage) {
    markPoint(eventName, stage, mDataAttribution);
  }

  /** Mark the event with given name, stage (start/end), and given data attribution. */
  public void markPoint(String eventName, String stage, String dataAttribution) {
    if (stage.equals(START)) {
      mStartedEvents.add(getFullMarkerName(eventName, dataAttribution, ""));
    } else if (stage.equals(END)
        && !mStartedEvents.remove(getFullMarkerName(eventName, dataAttribution, ""))) {
      // no matching start point, skip (can happen for changeset end)
      return;
    }
    markPoint(getFullMarkerName(eventName, dataAttribution, stage));
  }

  /** Callback to log the event point. */
  protected abstract void onMarkPoint(String name);

  /** @return whether this logger is active. */
  protected abstract boolean isEnabled();
}
