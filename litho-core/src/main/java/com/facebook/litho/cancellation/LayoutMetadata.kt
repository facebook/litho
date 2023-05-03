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

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.ResolveResult

/** This metadata encompasses all characteristics associated to a specific `Layout` request. */
@DataClassGenerate
data class LayoutMetadata(
    val localVersion: Int,
    val widthSpec: Int,
    val heightSpec: Int,
    val resolveResult: ResolveResult?,
    val executionMode: ExecutionMode
) {

  fun isEquivalentTo(layoutMetadata: LayoutMetadata): Boolean {
    if (widthSpec != layoutMetadata.widthSpec) return false
    if (heightSpec != layoutMetadata.heightSpec) return false
    if (resolveResult != layoutMetadata.resolveResult) return false

    return true
  }
}
