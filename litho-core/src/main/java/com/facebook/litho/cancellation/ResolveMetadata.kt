// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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

package com.facebook.litho.cancellation

import com.facebook.litho.LayoutState
import com.facebook.litho.TreeProps

/** This metadata encompasses all characteristics associated to a specific `Resolve` request. */
data class ResolveMetadata(
    val localVersion: Int,
    val componentId: Int,
    val treeProps: TreeProps?,
    val executionMode: ExecutionMode,
    @LayoutState.CalculateLayoutSource val source: Int
) {

  val id: Int = localVersion

  fun isEquivalentTo(resolveInput: ResolveMetadata): Boolean {
    if (componentId != resolveInput.componentId) return false
    if (treeProps != resolveInput.treeProps) return false

    return true
  }
}
