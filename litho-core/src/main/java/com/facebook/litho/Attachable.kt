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

/**
 * Interface for something (e.g. a Component or a useEffect registration) which should receive
 * callbacks when it is attached/detached from the tree.
 */
interface Attachable {

  /** @return an id that can uniquely identify an Attachable between different layouts */
  val uniqueId: String

  /**
   * Will be invoked when this Attachable becomes a committed part of the ComponentTree. E.g. for a
   * Component, when that Component first appears as part of a committed layout.
   */
  fun attach()

  /**
   * Will be invoked when this Attachable is no longer part of the ComponentTree. This can be
   * because the ComponentTree is released (and all Attachables are detached), or because this
   * Attachable no longer appears in the ComponentTree in the most recent committed layout.
   */
  fun detach()

  /**
   * Given an Attachable (nextAttachable) with the same unique id as this Attachable and which is in
   * the next layout, return whether it should be attached instead of the current Attachable. If
   * true is returned, this Attachable will be detached and nextAttachable will be attached. If
   * false is returned, this Attachable will remain attached and nextAttachable will be thrown away.
   */
  fun shouldUpdate(nextAttachable: Attachable): Boolean

  /**
   * Legacy update behavior is that if [shouldUpdate(Attachable)] returns false, we should still
   * replace the Attachable with the latest version of the Attachable. Practically speaking, this is
   * needed for Layout Specs and the OnAttached/OnDetached API where OnAttached can be called with
   * the initial component props, but OnDetached is expected to be called with the latest Component
   * props.
   */
  fun useLegacyUpdateBehavior(): Boolean
}
