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

/**
 * Includes most of bookkeeping logic for {@link com.facebook.litho.LithoStartupLogger}
 * implementations. This base class does *not* record the log points, but rather the implementation
 * class decides how to log points in {@link #onMarkPoint(String)}.
 */
public abstract class BaseLithoStartupLogger implements LithoStartupLogger {

  private static final HashSet<String> NEEDS_THREAD_INFO = new HashSet<>();

  static {
    NEEDS_THREAD_INFO.add(CHANGESET_CALCULATION);
    NEEDS_THREAD_INFO.add(INIT_RANGE);
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

  protected abstract void onMarkPoint(String name);

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

  @Override
  public void setDataAttribution(String attribution) {
    mDataAttribution = attribution;
  }

  @Override
  public String getLatestDataAttribution() {
    return mDataAttribution;
  }

  @Override
  public void markPoint(String eventName, String stage) {
    markPoint(eventName, stage, mDataAttribution);
  }

  @Override
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
}
