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

import com.facebook.litho.Transition
import com.facebook.litho.internal.HookKey
import com.facebook.rendercore.transitions.TransitionUtils

internal sealed interface TransitionData {
  /**
   * List of transitions that will be applied to a component.
   *
   * These transitions, when available, will always be applied to the component.
   */
  val transitions: List<Transition>?

  /**
   * Map of [HookKey] to [TransitionWithDependency] definitions for a component.
   *
   * These are not actual transitions themselves, but rather a set of definitions that will be used
   * to generate the transitions. In fact, every transition in [optimisticTransitions] is a
   * direct/indirect result of evaluating these definitions.
   */
  val transitionsWithDependency: Map<HookKey, TransitionWithDependency>?

  /**
   * Optimistic list of transitions based on the [transitionsWithDependency] definitions
   *
   * Unlike [transitions], these transitions are only applied if certain conditions regarding the
   * dependencies of the corresponding [TransitionWithDependency] are met. Otherwise, they may be
   * discarded and re-evaluated on-demand.
   */
  val optimisticTransitions: List<Transition>?

  /**
   * Returns true if this [TransitionData] is empty.
   *
   * A [TransitionData] is considered empty if all of its [transitions], [transitionsWithDependency]
   * and [optimisticTransitions] collections are null or empty.
   */
  fun isEmpty(): Boolean =
      transitions.isNullOrEmpty() &&
          transitionsWithDependency.isNullOrEmpty() &&
          optimisticTransitions.isNullOrEmpty()
}

internal class MutableTransitionData : TransitionData {
  override var transitions: MutableList<Transition>? = null
  override var transitionsWithDependency: MutableMap<HookKey, TransitionWithDependency>? = null
  override var optimisticTransitions: MutableList<Transition>? = null

  fun addTransition(transition: Transition) {
    val transitions = transitions ?: mutableListOf<Transition>().also { transitions = it }
    TransitionUtils.addTransitions(transition, transitions)
  }

  fun addTransitionWithDependency(twd: TransitionWithDependency, optimisticResult: Transition?) {
    addTransitionWithDependency(twd)
    if (optimisticResult != null) addOptimisticTransition(optimisticResult)
  }

  fun add(transitionData: TransitionData) {
    val otherTransitions = transitionData.transitions.orEmpty()
    for (transition in otherTransitions) addTransition(transition)

    val otherTwds = transitionData.transitionsWithDependency.orEmpty()
    for ((_, twd) in otherTwds) addTransitionWithDependency(twd)

    val otherOptimisticTransitions = transitionData.optimisticTransitions.orEmpty()
    for (transition in otherOptimisticTransitions) addOptimisticTransition(transition)
  }

  private fun addTransitionWithDependency(twd: TransitionWithDependency) {
    val twds =
        transitionsWithDependency
            ?: mutableMapOf<HookKey, TransitionWithDependency>().also {
              transitionsWithDependency = it
            }
    twds[twd.identityKey] = twd
  }

  private fun addOptimisticTransition(transition: Transition) {
    val transitions =
        optimisticTransitions ?: mutableListOf<Transition>().also { optimisticTransitions = it }
    transitions.add(transition)
  }
}

internal fun TransitionData.asMutableData(): MutableTransitionData =
    if (this is MutableTransitionData) this else MutableTransitionData().also { it.add(this) }
