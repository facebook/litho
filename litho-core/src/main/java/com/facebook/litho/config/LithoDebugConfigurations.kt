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

package com.facebook.litho.config

import com.facebook.litho.BuildConfig

/** Holds configurations related with debugging utilities for Litho. */
object LithoDebugConfigurations {

  /**
   * Indicates whether this is an internal build. Note that the implementation of `BuildConfig ` *
   * that this class is compiled against may not be the one that is included in the APK. See:
   * [android_build_config](http://facebook.github.io/buck/rule/android_build_config.html).
   */
  private val IS_INTERNAL_BUILD: Boolean = BuildConfig.IS_INTERNAL_BUILD

  /** Debug option to highlight interactive areas in mounted components. */
  @JvmField var debugHighlightInteractiveBounds: Boolean = false

  /** Debug option to highlight mount bounds of mounted components. */
  @JvmField var debugHighlightMountBounds: Boolean = false

  /** When `true`, disables incremental mount globally. */
  @JvmField var isIncrementalMountGloballyDisabled: Boolean = false

  @JvmField var isDebugModeEnabled: Boolean = IS_INTERNAL_BUILD

  /**
   * Option to enabled debug mode. This will save extra data associated with each node and allow
   * more info about the hierarchy to be retrieved. Used to enable stetho integration. It is highly
   * discouraged to enable this in production builds. Due to how the Litho releases are distributed
   * in open source IS_INTERNAL_BUILD will always be false. It is therefore required to override
   * this value using your own application build configs. Recommended place for this is in a
   * Application subclass onCreate() method.
   */
  @JvmField var isRenderInfoDebuggingEnabled: Boolean = isDebugModeEnabled

  /** Lightweight tracking of component class hierarchy of MountItems. */
  @JvmField var isDebugHierarchyEnabled: Boolean = false

  /** When `true` ComponentTree records state change snapshots */
  @JvmField var isTimelineEnabled: Boolean = isDebugModeEnabled

  @JvmField var timelineDocsLink: String? = null
}
