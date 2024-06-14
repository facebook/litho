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

import com.facebook.litho.internal.HookKey
import com.facebook.litho.transition.TransitionWithDependency

/**
 * A lightweight data structure to keep track of some props from the last mounted LayoutState.
 *
 * The exact params that are tracked are essentially the ones needed to support Components' use of
 * [Diff] params. For example, this is needed by [useTransition] hook to retrieve an equivalent
 * [TransitionWithDependency] (if any) from the last mounted LayoutState.
 *
 * @param layoutStateId the id of the corresponding LayoutState.
 * @param transitionsWithDependency a map of [TransitionWithDependency] that were applied in the
 *   corresponding LayoutState.
 */
internal class LayoutStateLiteData(
    val layoutStateId: Int,
    private val transitionsWithDependency: Map<HookKey, TransitionWithDependency>?
) {

  fun getTransitionWithDependency(identityKey: HookKey): TransitionWithDependency? {
    return transitionsWithDependency?.get(identityKey)
  }

  companion object {
    @JvmField
    val NONE: LayoutStateLiteData =
        LayoutStateLiteData(LayoutState.NO_PREVIOUS_LAYOUT_STATE_ID, null)
  }
}
