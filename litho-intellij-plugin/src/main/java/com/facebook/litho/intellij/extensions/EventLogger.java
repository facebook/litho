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

package com.facebook.litho.intellij.extensions;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension point for other plugins to provide own mechanism of logging given events.
 *
 * @see "plugin.xml"
 */
public interface EventLogger {
  String PLUGIN_ID = "com.facebook.litho.intellij";
  // Metadata keys
  String KEY_PLUGIN_VERSION = "version";
  String KEY_CLASS = "class";
  String KEY_FILE = "file";
  String KEY_RED_SYMBOLS = "red_symbols";
  String KEY_RED_SYMBOLS_ALL = "all_red_symbols";
  String KEY_RED_SYMBOLS_RESOLVED = "resolved_red_symbols";
  String KEY_RESULT = "result";
  String KEY_TARGET = "target";
  String KEY_TIME_BIND_RED_SYMBOLS = "time_bind";
  String KEY_TIME_COLLECT_RED_SYMBOLS = "time_collect";
  String KEY_TIME_RESOLVE_RED_SYMBOLS = "time_resolve";
  String KEY_TYPE = "type";

  // Metadata values. Naming VALUE_EVENTNAME_KEYNAME_X
  String VALUE_COMPLETION_TARGET_CALL = "call";
  String VALUE_COMPLETION_TARGET_METHOD = "method";
  String VALUE_COMPLETION_TARGET_PARAMETER = "parameter";
  String VALUE_COMPLETION_TARGET_ARGUMENT = "argument";
  String VALUE_NAVIGATION_TARGET_CLASS = "class";
  String VALUE_NAVIGATION_TARGET_METHOD = "method";
  String VALUE_NAVIGATION_TARGET_PARAMETER = "parameter";
  String VALUE_NAVIGATION_TYPE_FIND_USAGES = "find_usages";
  String VALUE_NAVIGATION_TYPE_GOTO = "goto";

  // Event types
  String EVENT_ANNOTATOR = "warning";
  String EVENT_COMPLETION = "completion";
  String EVENT_FIX_EVENT_HANDLER = "fix.event_handler";
  String EVENT_GOTO_GENERATED = "goto_generated";
  String EVENT_NAVIGATION = "navigation";
  String EVENT_NEW_TEMPLATE = "file_template";
  String EVENT_ON_EVENT_GENERATION = "event.generation";
  String EVENT_RED_SYMBOLS = "resolve_redsymbols";
  String EVENT_SETTINGS = "settings.update";
  String EVENT_TOOLWINDOW = "toolwindow";

  /**
   * Logs given event.
   *
   * @param event given event name
   */
  default void log(String event) {
    log(event, new HashMap<>());
  }

  void log(String event, Map<String, String> metadata);
}
