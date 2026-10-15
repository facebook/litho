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

package com.facebook.litho.animation

import androidx.annotation.VisibleForTesting
import com.facebook.litho.dataflow.BindingListener
import com.facebook.litho.dataflow.GraphBinding
import com.facebook.litho.dataflow.ValueNode

/**
 * Base class for defining animations for transitions between states of the component hierarchy.
 * Subclasses should define their animation by creating a [GraphBinding] in [setupBinding].
 */
abstract class TransitionAnimationBinding
@VisibleForTesting
internal constructor(private val graphBinding: GraphBinding) : BaseAnimationBinding() {

  constructor() : this(GraphBinding.create())

  init {
    graphBinding.setListener(
        object : BindingListener {
          override fun onAllNodesFinished(binding: GraphBinding) {
            this@TransitionAnimationBinding.onAllNodesFinished()
          }
        },
    )
  }

  /** @see GraphBinding.addBinding */
  fun addBinding(fromNode: ValueNode, toNode: ValueNode, inputName: String) {
    graphBinding.addBinding(fromNode, toNode, inputName)
  }

  /** @see GraphBinding.addBinding */
  fun addBinding(fromNode: ValueNode, toNode: ValueNode) {
    graphBinding.addBinding(fromNode, toNode)
  }

  override fun start(resolver: Resolver) {
    if (!shouldStart()) {
      notifyCanceledBeforeStart()
      return
    }
    notifyWillStart()

    setupBinding(resolver)
    graphBinding.activate()
  }

  /**
   * Subclasses should set up their animation by creating a graph that defines how data will flow to
   * relevant [AnimatedPropertyNode]s.
   */
  protected abstract fun setupBinding(resolver: Resolver)

  override fun stop() {
    if (!isActive()) {
      return
    }
    graphBinding.deactivate()
  }

  override fun isActive(): Boolean = graphBinding.isActive

  override fun prepareToStartLater() {
    notifyScheduledToStartLater()
  }

  private fun onAllNodesFinished() {
    notifyFinished()
    stop()
  }
}
