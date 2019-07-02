/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.litho.intellij.extensions;

import java.util.Collections;
import java.util.Map;

/**
 * Extension point for other plugins to provide own mechanism of logging given events.
 *
 * @see "plugin.xml"
 */
public interface EventLogger {

  String EVENT_ON_EVENT_GENERATION = "event.generation";
  String EVENT_ON_EVENT_COMPLETION = "event.completion";
  String EVENT_ANNOTATOR = "error.annotation";
  String EVENT_NEW_TEMPLATE = "file.template";
  String EVENT_GOTO_NAVIGATION = "goto.navigation";
  String EVENT_UPDATE_COMPONENT = "update.component";

  /**
   * Logs given event.
   *
   * @param event given event name
   */
  default void log(String event) {
    log(event, Collections.emptyMap());
  }

  void log(String event, Map<String, String> metadata);
}
