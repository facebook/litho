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

package com.facebook.litho.intellij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.vcs.log.util.StopWatch;

public class IntervalLogger {
  private final Logger logger;
  private long start;

  public IntervalLogger(Logger logger) {
    this.logger = logger;
    this.start = System.currentTimeMillis();
  }

  public void logStep(String action) {
    final long current = System.currentTimeMillis();
    logger.debug(StopWatch.formatTime(current - this.start) + " for " + action);
    this.start = current;
  }
}
