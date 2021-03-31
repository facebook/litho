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

import com.facebook.litho.config.ComponentsConfiguration;

/** Default implementation of ErrorEvent handler. */
public class DefaultErrorEventHandler extends ErrorEventHandler {

  private static final String SWALLOW_UNHANLED_EXCEPTIONS = "DefaultErrorEventHandler";

  static final DefaultErrorEventHandler INSTANCE = new DefaultErrorEventHandler();

  @Override
  public void onError(Exception e) {
    if (ComponentsConfiguration.swallowUnhandledExceptions) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          SWALLOW_UNHANLED_EXCEPTIONS,
          "Swallowing exception in experiment: " + e.getMessage());
      return;
    }
    ComponentUtils.rethrow(e);
  }
}
