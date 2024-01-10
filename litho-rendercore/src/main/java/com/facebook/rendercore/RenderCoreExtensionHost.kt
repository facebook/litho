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

import com.facebook.rendercore.extensions.RenderCoreExtension

/**
 * This interface collates the APIs which maybe used by a [RenderCoreExtension]. This allow both
 * [RootHost] and [RenderTreeHost] remain distinct while sharing APIs common to between them.
 */
interface RenderCoreExtensionHost {
  /** Notifies the host the its visible bounds may have potentially changed. */
  fun notifyVisibleBoundsChanged()

  /**
   * Notifies the host when its parent wants to start pre-mounting content.
   *
   * @param frameTimeMs the latest frame time
   */
  fun onRegisterForPremount(frameTimeMs: Long?)

  /** Notifies the host when its parent wants to stop pre-mounting content. */
  fun onUnregisterForPremount()

  /** Sets a [RenderTreeUpdateListener] on the [RootHost]'s [MountState] */
  fun setRenderTreeUpdateListener(listener: RenderTreeUpdateListener?)
}
