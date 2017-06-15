/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

/**
 * In test environments we don't want to recycle the events as mockity hold on to them.
 * We therefor override log() to not call release. 
 */
public class TestComponentsLogger extends BaseComponentsLogger {

  @Override
  public void log(LogEvent event) {
    if (event.isPerformanceEvent()) {
      onPerformanceEventEnded(event);
    } else {
      onEvent(event);
    }
  }

  @Override
  public void onPerformanceEventStarted(LogEvent event) {

  }

  @Override
  public void onPerformanceEventEnded(LogEvent event) {

  }

  @Override
  public void onEvent(LogEvent event) {

  }
}
