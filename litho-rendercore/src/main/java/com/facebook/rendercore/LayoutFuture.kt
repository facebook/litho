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

import android.content.Context
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.thread.utils.ThreadInheritingPriorityFuture
import java.util.concurrent.Callable

class LayoutFuture<State, RenderContext>(
    context: Context,
    renderContext: RenderContext?,
    val tree: Node<RenderContext>,
    state: State?,
    val version: Int,
    val frameId: Int,
    previousResult: RenderResult<State, RenderContext>?,
    extensions: Array<RenderCoreExtension<*, *>>?,
    val sizeConstraints: SizeConstraints
) :
    ThreadInheritingPriorityFuture<RenderResult<State, RenderContext>>(
        Callable {
          if (previousResult != null &&
              RenderResult.shouldReuseResult(tree, sizeConstraints, previousResult)) {
            RenderResult(previousResult.renderTree, tree, previousResult.layoutCacheData, state)
          } else {
            RenderResult.layout(
                RenderResult.createLayoutContext(
                    previousResult, renderContext, context, version, extensions),
                tree,
                state,
                sizeConstraints)
          }
        },
        "LayoutFuture")
