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

import java.util.concurrent.atomic.AtomicInteger

class StateUpdateTestComponent : SpecGeneratedComponent("StateUpdateTest") {

  private val createInitialStateCount = AtomicInteger(0)

  override fun isEquivalentProps(other: Component?, shouldCompareCommonProps: Boolean): Boolean =
      this === other

  override fun hasState(): Boolean = true

  override fun createInitialState(c: ComponentContext, stateContainer: StateContainer) {
    val testStateContainer = stateContainer as TestStateContainer
    testStateContainer.count = INITIAL_COUNT_STATE_VALUE
    createInitialStateCount.incrementAndGet()
    finalCounterValue.set(INITIAL_COUNT_STATE_VALUE)
  }

  fun getCount(c: ComponentContext?): Int = finalCounterValue.get()

  val componentForStateUpdate: StateUpdateTestComponent
    get() = this

  override fun createStateContainer(): StateContainer = TestStateContainer()

  protected fun getStateContainerImpl(c: ComponentContext): TestStateContainer? =
      c.scopedComponentInfo.stateContainer as TestStateContainer?

  class TestStateContainer : StateContainer() {
    @JvmField var count = 0

    override fun applyStateUpdate(stateUpdate: StateUpdate) {
      when (stateUpdate.type) {
        STATE_UPDATE_TYPE_NOOP -> {}
        STATE_UPDATE_TYPE_INCREMENT -> count += 1
        STATE_UPDATE_TYPE_MULTIPLY -> count *= 2
      }
      finalCounterValue.set(count)
    }
  }

  companion object {
    private const val STATE_UPDATE_TYPE_NOOP = 0
    private const val STATE_UPDATE_TYPE_INCREMENT = 1
    private const val STATE_UPDATE_TYPE_MULTIPLY = 2
    const val INITIAL_COUNT_STATE_VALUE = 4

    @JvmStatic
    fun createNoopStateUpdate(): StateContainer.StateUpdate =
        StateContainer.StateUpdate(STATE_UPDATE_TYPE_NOOP)

    @JvmStatic
    fun createIncrementStateUpdate(): StateContainer.StateUpdate =
        StateContainer.StateUpdate(STATE_UPDATE_TYPE_INCREMENT)

    @JvmStatic
    fun createMultiplyStateUpdate(): StateContainer.StateUpdate =
        StateContainer.StateUpdate(STATE_UPDATE_TYPE_MULTIPLY)

    private val idGenerator = AtomicInteger(0)
    private val finalCounterValue = AtomicInteger(0)
  }
}
