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

package com.facebook.rendercore

/** Config class to enable or disable specific features. */
object RenderCoreConfig {

  /**
   * Defaults to the presence of an
   * <pre>IS_TESTING</pre>
   *
   * system property at startup but can be overridden at runtime.
   */
  @JvmField var isEndToEndTestRun: Boolean = System.getProperty("IS_TESTING") != null

  /**
   * Enables the global gap worker which will schedule gaps between mountable items in the same
   * frame.
   */
  @JvmField var useGlobalGapWorker: Boolean = true

  /**
   * Enabling this config will fix the issue that component matches host view size will never be
   * unmounted when getting out of the viewport.
   */
  @JvmField var shouldEnableIMFix: Boolean = false

  /** Enabling unmounting components reversely from bottom to top. */
  @JvmField var enableUnmountingFromLeafNode: Boolean = false

  /** This flag is used to disable incremental unmounting in LithoView. */
  @JvmField var disableIncrementalUnmounting: Boolean = false

  /** This flag is used to enable automatic removal of View listeners from ComponentHost. */
  @JvmField var removeComponentHostListeners: Boolean = false
}
