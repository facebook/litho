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

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Set of events and event params logged by the framework. */
public interface FrameworkLogEvents {
  int EVENT_PRE_ALLOCATE_MOUNT_CONTENT = 8;
  int EVENT_INIT_RANGE = 20;
  int EVENT_CALCULATE_RESOLVE = 22;

  /**
   * This corresponds to the process of resolving a Component
   * (LayoutSpecs/KComponents/Mountables/Primitives)
   */
  int EVENT_COMPONENT_RESOLVE = 23;

  /** This corresponds to the process of calling `onPrepare` for Mountables/Primitives. */
  int EVENT_COMPONENT_PREPARE = 24;

  @IntDef({
    FrameworkLogEvents.EVENT_PRE_ALLOCATE_MOUNT_CONTENT,
    FrameworkLogEvents.EVENT_INIT_RANGE,
    FrameworkLogEvents.EVENT_CALCULATE_RESOLVE,
    FrameworkLogEvents.EVENT_COMPONENT_RESOLVE,
    FrameworkLogEvents.EVENT_COMPONENT_PREPARE,
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface LogEventId {}

  String PARAM_COMPONENT = "component";
  String PARAM_LOG_TAG = "log_tag";
  String PARAM_IS_BACKGROUND_LAYOUT = "is_background_layout";
  String PARAM_ATTRIBUTION = "attribution";
  String PARAM_SECTION_CURRENT = "section_current";
  String PARAM_SECTION_NEXT = "section_next";
  String PARAM_IS_MAIN_THREAD = "is_main_thread";
  String PARAM_VERSION = "version";
  String PARAM_SOURCE = "source";
}
