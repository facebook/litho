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

package com.facebook.litho.widget

import android.graphics.Rect
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.FocusedVisibleEvent
import com.facebook.litho.FullImpressionVisibleEvent
import com.facebook.litho.InvisibleEvent
import com.facebook.litho.LifecycleStep
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.UnfocusedVisibleEvent
import com.facebook.litho.VisibilityChangedEvent
import com.facebook.litho.VisibleEvent
import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnAttached
import com.facebook.litho.annotations.OnCalculateCachedValue
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnCreateTreeProp
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.OnUpdateStateWithTransition
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.events.EventWithoutAnnotation

@LayoutSpec(events = [EventWithoutAnnotation::class])
object LayoutSpecLifecycleTesterSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      state: StateValue<String?>,
      @Prop steps: MutableList<StepInfo>
  ) {
    steps.add(StepInfo(LifecycleStep.ON_CREATE_INITIAL_STATE))
    state.set("hello world")
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop steps: MutableList<StepInfo>,
      @Prop(optional = true) caller: Caller?,
      @Prop(optional = true) body: Component?,
      @State state: String?,
      @CachedValue expensiveValue: Int
  ): Component {
    steps.add(StepInfo(LifecycleStep.ON_CREATE_LAYOUT))
    checkNotNull(state) { "OnCreateLayout called without initialised state." }
    caller?.set(c, steps)
    return Column.create(c)
        .visibleHandler(LayoutSpecLifecycleTester.onVisible(c))
        .focusedHandler(LayoutSpecLifecycleTester.onFocusedVisible(c))
        .invisibleHandler(LayoutSpecLifecycleTester.onInvisible(c))
        .unfocusedHandler(LayoutSpecLifecycleTester.onUnfocusedVisibleEvent(c))
        .fullImpressionHandler(LayoutSpecLifecycleTester.onFullImpressionVisibleEvent(c))
        .visibilityChangedHandler(LayoutSpecLifecycleTester.onVisibilityChangedEvent(c))
        .child(body)
        .build()
  }

  @JvmStatic
  @OnCalculateCachedValue(name = "expensiveValue")
  fun onCalculateExpensiveValue(@Prop steps: MutableList<StepInfo>): Int {
    steps.add(StepInfo(LifecycleStep.ON_CALCULATE_CACHED_VALUE))
    return 0
  }

  @JvmStatic
  @OnCreateTreeProp
  fun onCreateTreePropRect(c: ComponentContext, @Prop steps: MutableList<StepInfo>): Rect {
    steps.add(StepInfo(LifecycleStep.ON_CREATE_TREE_PROP))
    return Rect()
  }

  @JvmStatic
  @OnCreateTransition
  fun onCreateTransition(c: ComponentContext, @Prop steps: MutableList<StepInfo>): Transition {
    steps.add(StepInfo(LifecycleStep.ON_CREATE_TRANSITION))
    return Transition.allLayout()
  }

  @JvmStatic
  @OnAttached
  fun onAttached(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_ATTACHED))
  }

  @JvmStatic
  @OnDetached
  fun onDetached(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_DETACHED))
  }

  @JvmStatic
  @OnUpdateState
  fun updateState(@Param stepsAsParam: MutableList<StepInfo>) {
    stepsAsParam.add(StepInfo(LifecycleStep.ON_UPDATE_STATE))
  }

  @JvmStatic
  @OnUpdateStateWithTransition
  fun updateStateWithTransition(@Param stepsAsParam: MutableList<StepInfo>): Transition {
    stepsAsParam.add(StepInfo(LifecycleStep.ON_UPDATE_STATE_WITH_TRANSITION))
    return Transition.allLayout()
  }

  @JvmStatic
  @OnEvent(EventWithoutAnnotation::class)
  fun onEventWithoutAnnotation(
      c: ComponentContext,
      @Prop(optional = true) caller: Caller?,
      @FromEvent count: Int,
      @FromEvent isDirty: Boolean,
      @FromEvent message: String?
  ) {
    caller?.eventWithoutAnnotation = EventWithoutAnnotation(count, isDirty, message)
  }

  @JvmStatic
  @OnEvent(VisibleEvent::class)
  fun onVisible(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_EVENT_VISIBLE))
  }

  @JvmStatic
  @OnEvent(FocusedVisibleEvent::class)
  fun onFocusedVisible(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE))
  }

  @JvmStatic
  @OnEvent(InvisibleEvent::class)
  fun onInvisible(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_EVENT_INVISIBLE))
  }

  @JvmStatic
  @OnEvent(UnfocusedVisibleEvent::class)
  fun onUnfocusedVisibleEvent(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE))
  }

  @JvmStatic
  @OnEvent(FullImpressionVisibleEvent::class)
  fun onFullImpressionVisibleEvent(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT))
  }

  @JvmStatic
  @OnEvent(VisibilityChangedEvent::class)
  fun onVisibilityChangedEvent(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_VISIBILITY_CHANGED))
  }

  class Caller {
    lateinit var c: ComponentContext
    lateinit var steps: List<StepInfo>

    var eventWithoutAnnotation: EventWithoutAnnotation? = null

    operator fun set(c: ComponentContext, steps: List<StepInfo>) {
      this.c = c
      this.steps = steps
    }

    fun updateStateSync() {
      LayoutSpecLifecycleTester.updateStateSync(c, steps)
    }

    fun updateStateWithTransition() {
      LayoutSpecLifecycleTester.updateStateWithTransitionWithTransition(c, steps)
    }

    fun dispatchEventWithoutAnnotation(event: EventWithoutAnnotation) {
      LayoutSpecLifecycleTester.onEventWithoutAnnotation(c).call(event)
    }
  }
}
