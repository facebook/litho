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

import com.facebook.litho.TransitionManager.AnimationCleanupTrigger
import com.facebook.litho.TransitionManager.AnimationState
import com.facebook.litho.TransitionManager.changeTypeToString

/**
 * This class aids to collect information about which animation states were removed and where from.
 * The idea is that we can retrieve this information when the inconsistency crash happens and report
 * it in a custom exception.
 *
 * This debugger is disabled by default and to be enabled clients should use
 * [AnimationInconsistencyDebuggerConfig.isEnabled] and then get an instance of it.
 */
internal class AnimationInconsistencyDebugger private constructor() {

  private val removedInfo = LinkedHashMap<TransitionId, RemovalInfo>()

  fun trackAnimationStateRemoved(
      transitionId: TransitionId,
      animationFinishTrigger: AnimationCleanupTrigger,
      animationState: AnimationState?
  ) {
    removedInfo[transitionId] = RemovalInfo(transitionId, animationFinishTrigger, animationState)
  }

  fun trackAnimationStateCreated(transitionId: TransitionId) {
    removedInfo.remove(transitionId)
  }

  fun reset(transitionIdMap: TransitionIdMap<AnimationState>) {
    transitionIdMap.ids().forEach { id ->
      val animationState = transitionIdMap[id]
      removedInfo[id] = RemovalInfo(id, AnimationCleanupTrigger.RESET, animationState)
    }
  }

  fun getReadableStatus(): String {
    return removedInfo.entries.joinToString(separator = ",") { it.value.toString() }
  }

  class RemovalInfo(
      val transitionId: TransitionId,
      private val animationCleanupTrigger: AnimationCleanupTrigger,
      private val animationState: AnimationState?
  ) {

    override fun toString(): String {
      return buildString {
        var currentIndent = 0
        fun indentLine(value: String) {
          append("  ".repeat(currentIndent)).appendLine(value)
        }

        indentLine("[")
        currentIndent++

        indentLine("transitionId=$transitionId")
        indentLine("animationCleanupTrigger=$animationCleanupTrigger")

        if (animationState != null) {
          indentLine("changeType=${changeTypeToString(animationState.changeType)}")
          indentLine(
              "shouldFinishUndeclaredAnimation=${animationState.shouldFinishUndeclaredAnimation}")
          indentLine("seenInLastTransition=${animationState.seenInLastTransition}")
          indentLine("hasDisappearingAnimation=${animationState.hasDisappearingAnimation}")
          if (animationState.propertyStates.isNotEmpty()) {
            indentLine("propertyStates:")
            currentIndent++
            animationState.propertyStates.forEach { (property, state) ->
              indentLine("${property.getName()}:")
              currentIndent++
              indentLine("targetValue=${state.targetValue}")
              indentLine("lastMountedValue=${state.lastMountedValue}")
              indentLine("numPendingAnimations=${state.numPendingAnimations}")
              currentIndent--
            }
          }
        }
        indentLine("]")
      }
    }
  }

  companion object Factory {

    @JvmName("get")
    internal fun get(): AnimationInconsistencyDebugger? {
      return if (AnimationInconsistencyDebuggerConfig.isEnabled) {
        AnimationInconsistencyDebugger()
      } else {
        null
      }
    }
  }
}

object AnimationInconsistencyDebuggerConfig {

  @JvmField var isEnabled = false
}
