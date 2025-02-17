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

class RenderResultFuture<State, RenderContext>(
    @Volatile private var previousResult: RenderResult<State, RenderContext>?,
    val setRootId: Int,
    val sizeConstraints: SizeConstraints,
    callable: Callable<RenderResult<State, RenderContext>>
) :
    ThreadInheritingPriorityFuture<RenderResult<State, RenderContext>>(
        callable, "RenderResultFuture") {

  @Deprecated(message = "Use the constructor that accepts SizeConstraints")
  constructor(
      context: Context,
      resolveResult: ResolveResult<Node<RenderContext>, State>,
      renderContext: RenderContext?,
      extensions: Array<RenderCoreExtension<*, *>>?,
      previousResult: RenderResult<State, RenderContext>?,
      setRootId: Int,
      widthSpec: Int,
      heightSpec: Int
  ) : this(
      context,
      resolveResult,
      renderContext,
      extensions,
      previousResult,
      setRootId,
      SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec))

  constructor(
      context: Context,
      resolveResult: ResolveResult<Node<RenderContext>, State>,
      renderContext: RenderContext?,
      extensions: Array<RenderCoreExtension<*, *>>?,
      previousResult: RenderResult<State, RenderContext>?,
      setRootId: Int,
      sizeConstraints: SizeConstraints
  ) : this(
      previousResult,
      setRootId,
      sizeConstraints,
      Callable<RenderResult<State, RenderContext>> {
        RenderResult.render<State, RenderContext>(
            context,
            resolveResult,
            renderContext,
            extensions,
            previousResult,
            setRootId,
            sizeConstraints)
      })

  constructor(
      previousResult: RenderResult<State, RenderContext>?,
      setRootId: Int,
      widthSpec: Int,
      heightSpec: Int,
      callable: Callable<RenderResult<State, RenderContext>>
  ) : this(
      previousResult, setRootId, SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec), callable)

  override fun onResultReady(result: RenderResult<State, RenderContext>) {
    super.onResultReady(result)
    previousResult = null
  }

  val latestAvailableRenderResult: RenderResult<State, RenderContext>?
    get() = if (isDone) runAndGet() else previousResult
}
