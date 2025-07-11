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

import com.facebook.rendercore.RenderState.ResolveFunc
import com.facebook.rendercore.StateUpdateReceiver.StateUpdate
import com.facebook.rendercore.thread.utils.PriorityInheritingFuture
import java.util.concurrent.Callable

class ResolveFuture<State, RenderContext, StateUpdateType : StateUpdate<*>>(
    resolveFunc: ResolveFunc<State, RenderContext, StateUpdateType>,
    resolveContext: ResolveContext<RenderContext, StateUpdateType>,
    committedTree: Node<RenderContext>?,
    committedState: State?,
    val stateUpdatesToApply: List<StateUpdateType>,
    val version: Int,
    val frameId: Int,
) :
    PriorityInheritingFuture<ResolveResult<Node<RenderContext>, State>>(
        "ResolveFuture",
        Callable {
          resolveFunc.resolve(resolveContext, committedTree, committedState, stateUpdatesToApply)
        })
