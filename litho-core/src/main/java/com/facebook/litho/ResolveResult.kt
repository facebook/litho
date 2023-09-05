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

import android.util.Pair
import com.facebook.litho.Resolver.Outputs
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.JvmField

class ResolveResult(
    @JvmField val node: LithoNode?,
    @JvmField val context: ComponentContext,
    @JvmField val component: Component,
    cache: MeasuredResultCache,
    @JvmField val treeState: TreeState,
    override val isPartialResult: Boolean,
    @JvmField val version: Int,
    @JvmField val eventHandlers: List<Pair<String, EventHandler<*>>>?,
    @JvmField val outputs: Outputs?,
    @JvmField val contextForResuming: ResolveContext?
) : PotentiallyPartialResult {

  private val cache: AtomicReference<MeasuredResultCache> = AtomicReference(cache)

  fun consumeCache(): MeasuredResultCache = cache.getAndSet(MeasuredResultCache.EMPTY)
}
