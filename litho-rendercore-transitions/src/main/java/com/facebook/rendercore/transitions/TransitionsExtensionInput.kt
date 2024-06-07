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

package com.facebook.rendercore.transitions

import com.facebook.litho.AnimatableItem
import com.facebook.litho.OutputUnitsAffinityGroup
import com.facebook.litho.Transition
import com.facebook.litho.Transition.RootBoundsTransition
import com.facebook.litho.TransitionId
import com.facebook.rendercore.MountDelegateInput
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.Systracer

/**
 * Delegate Input needs to implement this interface to provide access to specific transitions
 * information.
 */
interface TransitionsExtensionInput : MountDelegateInput {

  val treeId: Int
  val tracer: Systracer?
  val isIncrementalMountEnabled: Boolean
  val rootName: String?
  val rootTransitionId: TransitionId?
  val animatableRootItem: AnimatableItem?
  val transitions: List<Transition?>?
  val transitionIdMapping: Map<TransitionId, OutputUnitsAffinityGroup<AnimatableItem>>?

  fun getMountTimeTransitions(): List<Transition?>?

  // TODO: remove dependency to MountDelegateInput
  override fun getMountableOutputCount(): Int

  override fun getMountableOutputAt(index: Int): RenderTreeNode?

  fun needsToRerunTransitions(): Boolean

  fun setNeedsToRerunTransitions(needsToRerunTransitions: Boolean)

  fun getAnimatableItemForTransitionId(
      transitionId: TransitionId
  ): OutputUnitsAffinityGroup<AnimatableItem>?

  fun getAnimatableItem(id: Long): AnimatableItem?

  fun renderUnitWithIdHostsRenderTrees(id: Long): Boolean

  fun setInitialRootBoundsForAnimation(
      rootWidth: RootBoundsTransition?,
      rootHeight: RootBoundsTransition?
  )
}
