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

import java.lang.Exception

/**
 * Implementation of [Attachable] for layout specs. Handles dispatching
 * [com.facebook.litho.annotations.OnAttached] and [com.facebook.litho.annotations.OnDetached] to
 * the given Component when the Attachable is attached/detached.
 */
internal class LayoutSpecAttachable(
    override val uniqueId: String,
    private val component: SpecGeneratedComponent,
    private val scopedComponentInfo: ScopedComponentInfo
) : Attachable {

  override fun attach() {
    val scopedContext = checkNotNull(scopedComponentInfo.context)
    try {
      component.onAttached(scopedContext)
    } catch (e: Exception) {
      ComponentUtils.handle(scopedContext, e)
    }
  }

  override fun detach() {
    val scopedContext = checkNotNull(scopedComponentInfo.context)
    try {
      component.onDetached(scopedContext)
    } catch (e: Exception) {
      ComponentUtils.handle(scopedContext, e)
    }
  }

  override fun shouldUpdate(nextAttachable: Attachable): Boolean = false

  override fun useLegacyUpdateBehavior(): Boolean = true
}
