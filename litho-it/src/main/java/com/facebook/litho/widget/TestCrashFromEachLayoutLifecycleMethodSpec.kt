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
import com.facebook.litho.BoundaryWorkingRange
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.FocusedVisibleEvent
import com.facebook.litho.FullImpressionVisibleEvent
import com.facebook.litho.InvisibleEvent
import com.facebook.litho.LifecycleStep
import com.facebook.litho.Output
import com.facebook.litho.StateValue
import com.facebook.litho.TestTriggerEvent
import com.facebook.litho.Transition
import com.facebook.litho.VisibilityChangedEvent
import com.facebook.litho.VisibleEvent
import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.FromTrigger
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnAttached
import com.facebook.litho.annotations.OnCalculateCachedValue
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnCreateTreeProp
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.OnEnteredRange
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnExitedRange
import com.facebook.litho.annotations.OnLoadStyle
import com.facebook.litho.annotations.OnRegisterRanges
import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.OnUpdateStateWithTransition
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.events.EventWithoutAnnotation

/**
 * LayoutSpec that implements lifecycle methods and crashes from the one provided as crashFromStep
 * prop.
 */
@LayoutSpec(events = [EventWithoutAnnotation::class])
object TestCrashFromEachLayoutLifecycleMethodSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      state: StateValue<String>,
      @Prop crashFromStep: LifecycleStep
  ) {
    state.set("hello world")
    if (crashFromStep == LifecycleStep.ON_CREATE_INITIAL_STATE) {
      throw RuntimeException("onCreateInitialState crash")
    }
  }

  @JvmStatic
  @OnCreateTreeProp
  fun onCreateTreePropRect(c: ComponentContext, @Prop crashFromStep: LifecycleStep): Rect {
    if (crashFromStep == LifecycleStep.ON_CREATE_TREE_PROP) {
      throw RuntimeException("onCreateTreeProp crash")
    }
    return Rect()
  }

  @JvmStatic
  @OnCalculateCachedValue(name = "expensiveValue")
  fun onCalculateExpensiveValue(@Prop crashFromStep: LifecycleStep): Int {
    if (crashFromStep == LifecycleStep.ON_CALCULATE_CACHED_VALUE) {
      throw RuntimeException("onCalculateCachedValue crash")
    }
    return 0
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop crashFromStep: LifecycleStep,
      @Prop(optional = true) caller: Caller?,
      @State state: String,
      @CachedValue expensiveValue: Int
  ): Component {
    checkNotNull(state) { "OnCreateLayout called without initialised state." }
    caller?.set(c, crashFromStep)
    if (crashFromStep == LifecycleStep.ON_CREATE_LAYOUT) {
      throw RuntimeException("onCreateLayout crash")
    }
    return Column.create(c)
        .visibleHandler(TestCrashFromEachLayoutLifecycleMethod.onVisible(c))
        .invisibleHandler(TestCrashFromEachLayoutLifecycleMethod.onInvisible(c))
        .focusedHandler(TestCrashFromEachLayoutLifecycleMethod.onFocusedEventVisible(c))
        .fullImpressionHandler(
            TestCrashFromEachLayoutLifecycleMethod.onFullImpressionVisibleEvent(c))
        .visibilityChangedHandler(
            TestCrashFromEachLayoutLifecycleMethod.onVisibilityChangedEvent(c))
        .build()
  }

  @JvmStatic
  @OnLoadStyle
  fun onLoadStyle(c: ComponentContext, crashFromStep: Output<LifecycleStep>) {
    // no need for the if clause for crashFromStep, we invoke this method
    // via TestCrashFromEachLayoutLifecycleMethod.create(context, 0, R.style.Animation)
    throw RuntimeException("onLoadStyle crash")
  }

  @JvmStatic
  @OnCreateTransition
  fun onCreateTransition(c: ComponentContext, @Prop crashFromStep: LifecycleStep): Transition {
    if (crashFromStep == LifecycleStep.ON_CREATE_TRANSITION) {
      throw RuntimeException("onCreateTransition crash")
    }
    return Transition.allLayout()
  }

  @JvmStatic
  @OnAttached
  fun onAttached(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_ATTACHED) {
      throw RuntimeException("onAttached crash")
    }
  }

  @JvmStatic
  @OnDetached
  fun onDetached(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_DETACHED) {
      throw RuntimeException("onDetached crash")
    }
  }

  @JvmStatic
  @OnUpdateState
  fun updateState(@Param crashFromStepAsParam: LifecycleStep) {
    if (crashFromStepAsParam == LifecycleStep.ON_UPDATE_STATE) {
      throw RuntimeException("onUpdateState crash")
    }
  }

  @JvmStatic
  @OnUpdateStateWithTransition
  fun updateStateWithTransition(@Param crashFromStepAsParam: LifecycleStep): Transition {
    if (crashFromStepAsParam == LifecycleStep.ON_UPDATE_STATE_WITH_TRANSITION) {
      throw RuntimeException("onUpdateStateWithTransition crash")
    }
    return Transition.allLayout()
  }

  @JvmStatic
  @OnEvent(VisibleEvent::class)
  fun onVisible(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_EVENT_VISIBLE) {
      throw RuntimeException("onEventVisible crash")
    }
  }

  @JvmStatic
  @OnEvent(InvisibleEvent::class)
  fun onInvisible(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_EVENT_INVISIBLE) {
      throw RuntimeException("onEventInvisible crash")
    }
  }

  @JvmStatic
  @OnEvent(FocusedVisibleEvent::class)
  fun onFocusedEventVisible(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_FOCUSED_EVENT_VISIBLE) {
      throw RuntimeException("onFocusedEventVisible crash")
    }
  }

  @JvmStatic
  @OnEvent(FullImpressionVisibleEvent::class)
  fun onFullImpressionVisibleEvent(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT) {
      throw RuntimeException("onFullImpressionVisible crash")
    }
  }

  @JvmStatic
  @OnEvent(VisibilityChangedEvent::class)
  fun onVisibilityChangedEvent(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_VISIBILITY_CHANGED) {
      throw RuntimeException("onVisibilityChanged crash")
    }
  }

  @JvmStatic
  @OnRegisterRanges
  fun registerWorkingRanges(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_REGISTER_RANGES) {
      throw RuntimeException("onRegisterRanges crash")
    }
    TestCrashFromEachLayoutLifecycleMethod.registerBoundaryWorkingRange(c, BoundaryWorkingRange())
  }

  @JvmStatic
  @OnEnteredRange(name = "boundary")
  fun onEnteredWorkingRange(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_ENTERED_RANGE) {
      throw RuntimeException("onEnteredRange crash")
    }
  }

  @JvmStatic
  @OnExitedRange(name = "boundary")
  fun onExitedWorkingRange(c: ComponentContext, @Prop crashFromStep: LifecycleStep) {
    if (crashFromStep == LifecycleStep.ON_EXITED_RANGE) {
      throw RuntimeException("onExitedRange crash")
    }
  }

  @JvmStatic
  @OnTrigger(TestTriggerEvent::class)
  fun triggerTestEvent(
      c: ComponentContext,
      @Prop crashFromStep: LifecycleStep,
      @FromTrigger triggerObject: Any?
  ) {
    if (crashFromStep == LifecycleStep.ON_TRIGGER) {
      throw RuntimeException("onTrigger crash")
    }
  }

  class Caller {
    lateinit var c: ComponentContext
    lateinit var crashFromStep: LifecycleStep

    operator fun set(c: ComponentContext, crashFromStep: LifecycleStep) {
      this.c = c
      this.crashFromStep = crashFromStep
    }

    fun updateStateSync() {
      TestCrashFromEachLayoutLifecycleMethod.updateStateSync(c, crashFromStep)
    }

    fun updateStateWithTransition() {
      TestCrashFromEachLayoutLifecycleMethod.updateStateWithTransitionWithTransition(
          c, crashFromStep)
    }
  }
}
