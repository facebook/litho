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

package com.facebook.litho

import androidx.annotation.IntDef

/** Set of events and event params logged by the framework. */
interface FrameworkLogEvents {

  @IntDef(
      EVENT_PRE_ALLOCATE_MOUNT_CONTENT,
      EVENT_INIT_RANGE,
      EVENT_CALCULATE_RESOLVE,
      EVENT_COMPONENT_RESOLVE,
      EVENT_COMPONENT_PREPARE)
  @Retention(AnnotationRetention.SOURCE)
  annotation class LogEventId

  companion object {
    const val EVENT_PRE_ALLOCATE_MOUNT_CONTENT: Int = 8
    const val EVENT_INIT_RANGE: Int = 20
    const val EVENT_CALCULATE_RESOLVE: Int = 22

    /**
     * This corresponds to the process of resolving a Component
     * (LayoutSpecs/KComponents/Mountables/Primitives)
     */
    const val EVENT_COMPONENT_RESOLVE: Int = 23

    /** This corresponds to the process of calling `onPrepare` for Mountables/Primitives. */
    const val EVENT_COMPONENT_PREPARE: Int = 24

    const val PARAM_COMPONENT: String = "component"
    const val PARAM_LOG_TAG: String = "log_tag"
    const val PARAM_IS_BACKGROUND_LAYOUT: String = "is_background_layout"
    const val PARAM_ATTRIBUTION: String = "attribution"
    const val PARAM_SECTION_CURRENT: String = "section_current"
    const val PARAM_SECTION_NEXT: String = "section_next"
    const val PARAM_IS_MAIN_THREAD: String = "is_main_thread"
    const val PARAM_VERSION: String = "version"
    const val PARAM_SOURCE: String = "source"
  }
}
