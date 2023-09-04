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
import com.facebook.rendercore.LayoutCache

/**
 * Wraps objects which should only be available for the duration of a LayoutState, to access them in
 * other classes such as ComponentContext during layout state calculation. When the layout
 * calculation finishes, the LayoutState reference is nullified. Using a wrapper instead of passing
 * the instances directly helps with clearing out the reference from all objects that hold on to it,
 * without having to keep track of all these objects to clear out the references.
 */
class LithoLayoutContext
constructor(
    override val treeId: Int,
    override val cache: MeasuredResultCache,
    rootContext: ComponentContext?,
    treeState: TreeState?,
    override val layoutVersion: Int,
    override val rootComponentId: Int,
    override val isAccessibilityEnabled: Boolean,
    val layoutCache: LayoutCache,
    currentDiffTree: DiffNode?,
    layoutStateFuture: TreeFuture<*>?
) : CalculationContext {

  private var _treeState: TreeState? = treeState
  private var _future: TreeFuture<*>? = layoutStateFuture
  private var _rootContext: ComponentContext? = rootContext
  private var _currentDiffTree: DiffNode? = currentDiffTree

  private var _eventHandlers: MutableList<Pair<String, EventHandler<*>>>? = null
  private var _currentNestedTreeDiffNode: DiffNode? = null

  override val treeFuture: TreeFuture<*>?
    get() {
      return _future
    }

  override val isFutureReleased: Boolean
    get() {
      val future = _future
      return future != null && future.isReleased
    }

  override val treeState: TreeState
    get() {
      return checkNotNull(_treeState)
    }

  override val eventHandlers: List<Pair<String, EventHandler<*>>>?
    get() {
      return _eventHandlers
    }

  override fun recordEventHandler(globalKey: String, eventHandler: EventHandler<*>) {
    (_eventHandlers ?: ArrayList()).apply {
      _eventHandlers = this
      add(Pair(globalKey, eventHandler))
    }
  }

  val rootComponentContext: ComponentContext?
    get() {
      return _rootContext
    }

  val currentDiffTree: DiffNode?
    get() {
      return _currentDiffTree
    }

  var isReleased: Boolean = false
    private set

  var perfEvent: PerfEvent? = null

  fun setNestedTreeDiffNode(diff: DiffNode?) {
    _currentNestedTreeDiffNode = diff
  }

  fun hasNestedTreeDiffNodeSet(): Boolean {
    return _currentNestedTreeDiffNode != null
  }

  fun consumeNestedTreeDiffNode(): DiffNode? {
    return _currentNestedTreeDiffNode.apply { _currentNestedTreeDiffNode = null }
  }

  fun release() {
    _treeState = null
    _future = null
    _currentDiffTree = null
    _rootContext = null
    perfEvent = null
    isReleased = true
  }
}
