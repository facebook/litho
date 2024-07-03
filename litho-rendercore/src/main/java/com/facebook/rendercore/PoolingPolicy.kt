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

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate

/**
 * Defines which behaviors the mount pool can allow regarding acquire and release. This allows for
 * fine-grained control, such that for example one can only acquire from the pool and not release
 * into it.
 *
 * This can be useful in specific cases where we would want to warm-up the pools with pre-allocation
 * but not recycle them back into the pool due to known issues with recycling.
 */
@DataClassGenerate
sealed class PoolingPolicy(
    @JvmField val canAcquireContent: Boolean,
    @JvmField val canReleaseContent: Boolean
) {

  data object Default : PoolingPolicy(canAcquireContent = true, canReleaseContent = true)

  data object Disabled : PoolingPolicy(canAcquireContent = false, canReleaseContent = false)

  data object AcquireOnly : PoolingPolicy(canAcquireContent = true, canReleaseContent = false)
}
