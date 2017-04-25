/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.v4.util.SimpleArrayMap;

/**
 * A class representing an event to log to a {@link ComponentsLogger}. There are two kind of events,
 * performance events and regular events. Performance events track the time between instantiation
 * and being logged while regular events do not. A {@link LogEvent} is created by requesting a new
 * event from the {@link ComponentsLogger} and they are recycled when logged and should not be
 * re-used.
 */
public final class LogEvent {
  private SimpleArrayMap<String, Object> mParams = new SimpleArrayMap<>();
  private int mEventId = -1;
  private boolean mIsPerformanceEvent = false;

  LogEvent() {}

  void setEventId(int eventId) {
    mEventId = eventId;
  }

  void setIsPerformanceEvent(boolean performanceEvent) {
    mIsPerformanceEvent = performanceEvent;
  }

  void reset() {
    mParams.clear();
    mEventId = -1;
    mIsPerformanceEvent = false;
  }

  public int getEventId() {
    return mEventId;
  }

  public boolean isPerformanceEvent() {
    return mIsPerformanceEvent;
  }

  public void addParam(String key, Object value) {
    mParams.put(key, value);
  }

  public int getParamCount() {
    return mParams.size();
  }

  public String getParamKeyAt(int index) {
    return mParams.keyAt(index);
  }

  public <T> T getParamValueAt(int index) {
    return (T) mParams.valueAt(index);
  }

  public <T> T getParam(String paramMessage) {
    return (T) mParams.get(paramMessage);
  }

  @Override
  public String toString() {
    return "eventId = " + mEventId +
        ", isPerformanceEvent = " + mIsPerformanceEvent +
        ", params = " + mParams.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof LogEvent) {
      final LogEvent other = (LogEvent) o;

      if (other.mEventId == mEventId && other.mIsPerformanceEvent == mIsPerformanceEvent) {
        for (int i = 0, count = mParams.size(); i < count; i++) {
          final String key = mParams.keyAt(i);

          if (other.mParams.containsKey(key)) {
            if (!mParams.get(key).equals(other.mParams.get(key))) {
              return false;
            }
          }
        }

        return true;
      }
    }

    return false;
  }
}
