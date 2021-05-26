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

  private static final String DEFAULT_ERROR_EVENT_HANDLER = "DefaultErrorEventHandler";

  static final DefaultErrorEventHandler INSTANCE = new DefaultErrorEventHandler();

  @Override
  public void onError(ComponentTree ct, Exception e) {
    if (ct != null && ct.getRoot() != null) {
      String categoryKey = DEFAULT_ERROR_EVENT_HANDLER + ":" + ct.getRoot().getSimpleName();
      if (e instanceof LithoMetadataExceptionWrapper) {
        Component crashingComponent = ((LithoMetadataExceptionWrapper) e).getCrashingComponent();
        if (crashingComponent != null) {
          categoryKey = categoryKey + ":" + crashingComponent.getSimpleName();
        }
      }
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR, categoryKey, e.getMessage());
    }

    if (ComponentsConfiguration.swallowUnhandledExceptions) {
      return;
    }
    ComponentUtils.rethrow(e);
  }
}
