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

package com.facebook.litho;

/** Default implementation of ErrorEvent handler. */
public class DefaultErrorEventHandler extends ErrorEventHandler {

  private static final String DEFAULT_ERROR_EVENT_HANDLER = "DefaultErrorEventHandler";

  static final DefaultErrorEventHandler INSTANCE = new DefaultErrorEventHandler();

  @Override
  public Component onError(ComponentContext cc, Exception e) {
    if (cc != null) {
      String categoryKey = DEFAULT_ERROR_EVENT_HANDLER + ":" + cc.getLogTag();
      if (e instanceof ReThrownException) {
        e = ((ReThrownException) e).original;
      }
      if (e instanceof LithoMetadataExceptionWrapper) {
        final String crashingComponentName =
            ((LithoMetadataExceptionWrapper) e).getCrashingComponentName();
        if (crashingComponentName != null) {
          categoryKey = categoryKey + ":" + crashingComponentName;
        }
      }
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR, categoryKey, e.getMessage());
    }

    ComponentUtils.rethrow(e);

    return null;
  }
}
