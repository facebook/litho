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

import android.util.SparseIntArray
import com.facebook.proguard.annotations.DoNotStrip
import java.util.ArrayList
import kotlin.collections.HashMap

class ScopedComponentInfo(
    val component: Component,
    val context: ComponentContext,
    errorEventHandler: EventHandler<ErrorEvent>?
) : Cloneable {

  var stateContainer: StateContainer? = null
  val prepareInterStagePropsContainer: PrepareInterStagePropsContainer? =
      if (component is SpecGeneratedComponent) component.createPrepareInterStagePropsContainer()
      else null

  /**
   * Holds onto how many direct component children of each type this Component has. Used for
   * automatically generating unique global keys for all sibling components of the same type.
   */
  private val childCounters: SparseIntArray by lazy { SparseIntArray(1) }

  /** Count the times a manual key is used so that clashes can be resolved. */
  @DoNotStrip private var manualKeysCounter: MutableMap<String, Int>? = null

  /**
   * Holds an event handler with its dispatcher set to the parent component, or - in case that this
   * is a root component - a default handler that reraises the exception. Null if the component
   * isn't initialized.
   */
  var errorEventHandler: EventHandler<ErrorEvent>? = errorEventHandler
    private set

  var commonProps: CommonProps? = null

  /**
   * Holds a list of working range related data. [LayoutState] will use it to update
   * [LayoutState.mWorkingRangeContainer] when calculate method is finished.
   */
  private val workingRangeRegistrations: MutableList<WorkingRangeContainer.Registration> by lazy {
    ArrayList()
  }

  /**
   * Returns the number of children of a given type @param[component] component has and then
   * increments it by 1.
   *
   * @param component the child component
   * @return the number of children components of type @param[component]
   */
  fun getChildCountAndIncrement(component: Component): Int {
    val count = childCounters.get(component.typeId, 0)
    childCounters.put(component.typeId, count + 1)
    return count
  }

  /**
   * Returns the number of children with same @param[manualKey] component has and then increments it
   * by 1.
   *
   * @param manualKey
   * @return the number of manual key usage
   */
  fun getManualKeyUsagesCountAndIncrement(manualKey: String): Int {
    val map = manualKeysCounter ?: HashMap<String, Int>(1).also { manualKeysCounter = it }
    val count = map[manualKey] ?: 0
    map[manualKey] = count + 1
    return count
  }

  /** Store a working range information into a list for later use by [LayoutState]. */
  fun registerWorkingRange(
      name: String,
      workingRange: WorkingRange,
      component: Component,
      globalKey: String
  ) {
    workingRangeRegistrations.add(WorkingRangeContainer.Registration(name, workingRange, this))
  }

  fun addWorkingRangeToNode(node: LithoNode) {
    if (CollectionsUtils.isNotNullOrEmpty(workingRangeRegistrations)) {
      node.addWorkingRanges(workingRangeRegistrations)
    }
  }

  /** This setter should only be called during the render phase of the component, never after. */
  fun setErrorEventHandlerDuringRender(errorHandler: EventHandler<ErrorEvent>?) {
    errorEventHandler = errorHandler
  }

  fun commitToLayoutState(treeState: TreeState) {
    // the get method adds the state container to the needed state container map
    treeState.keepStateContainerForGlobalKey(context.globalKey, context.isNestedTreeContext)
  }

  private fun hasState(): Boolean = component is SpecGeneratedComponent && component.hasState()

  override fun clone(): ScopedComponentInfo =
      try {
        super.clone() as ScopedComponentInfo
      } catch (e: CloneNotSupportedException) {
        throw RuntimeException(e)
      }
}
