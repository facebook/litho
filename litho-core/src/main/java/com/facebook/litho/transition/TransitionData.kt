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

package com.facebook.litho.transition

import com.facebook.litho.Component.RenderData
import com.facebook.litho.Transition
import com.facebook.rendercore.transitions.TransitionUtils

internal sealed interface TransitionData {
  /**
   * List of transitions that will be applied to a component.
   *
   * These transitions, when available, will always be applied to the component.
   */
  val transitions: List<Transition>?

  /**
   * List of [TransitionCreator] definitions for a component.
   *
   * These are not actual transitions themselves, but rather a set of definitions that will be used
   * to generate the transitions. In fact, every transition in [optimisticTransitions] is a
   * direct/indirect result of evaluating these definitions.
   */
  val transitionCreators: List<TransitionCreator>?

  /**
   * Optimistic list of transitions based on the [transitionCreators] definitions
   *
   * Unlike [transitions], these transitions are only applied if certain conditions regarding the
   * dependencies of the corresponding [TransitionCreator] are met. Otherwise, they may be discarded
   * and re-evaluated on-demand.
   */
  val optimisticTransitions: List<Transition>?

  /**
   * Returns true if this [TransitionData] is empty.
   *
   * A [TransitionData] is considered empty if all of its [transitions], [transitionCreators] and
   * [optimisticTransitions] collections are null or empty.
   */
  fun isEmpty(): Boolean =
      transitions.isNullOrEmpty() &&
          transitionCreators.isNullOrEmpty() &&
          optimisticTransitions.isNullOrEmpty()
}

internal class MutableTransitionData : TransitionData {
  override var transitions: MutableList<Transition>? = null
  override var transitionCreators: MutableList<TransitionCreator>? = null
  override var optimisticTransitions: MutableList<Transition>? = null

  fun addTransition(transition: Transition) {
    val transitions = transitions ?: mutableListOf<Transition>().also { transitions = it }
    TransitionUtils.addTransitions(transition, transitions)
  }

  fun addTransitionCreator(creator: TransitionCreator, previousRenderData: RenderData? = null) {
    addTransitionCreator(creator)
    if (creator.supportsOptimisticTransitions) {
      val optimisticTransition = creator.createTransition(previousRenderData)
      if (optimisticTransition != null) addOptimisticTransition(optimisticTransition)
    }
  }

  fun add(transitionData: TransitionData) {
    transitionData.transitions?.let { other ->
      val transitions = transitions ?: ArrayList<Transition>(other.size).also { transitions = it }
      transitions.addAll(other)
    }
    transitionData.transitionCreators?.let { other ->
      val creators =
          transitionCreators
              ?: ArrayList<TransitionCreator>(other.size).also { transitionCreators = it }
      creators.addAll(other)
    }
    transitionData.optimisticTransitions?.let { other ->
      val optimisticTransitions =
          optimisticTransitions
              ?: ArrayList<Transition>(other.size).also { optimisticTransitions = it }
      optimisticTransitions.addAll(other)
    }
  }

  private fun addTransitionCreator(creator: TransitionCreator) {
    val creators =
        transitionCreators ?: mutableListOf<TransitionCreator>().also { transitionCreators = it }
    creators.add(creator)
  }

  private fun addOptimisticTransition(transition: Transition) {
    val transitions =
        optimisticTransitions ?: mutableListOf<Transition>().also { optimisticTransitions = it }
    transitions.add(transition)
  }
}

/** Creates and returns a new [MutableTransitionData] copy of this [TransitionData]. */
internal fun TransitionData.toMutableData(): MutableTransitionData =
    MutableTransitionData().also { it.add(this) }
