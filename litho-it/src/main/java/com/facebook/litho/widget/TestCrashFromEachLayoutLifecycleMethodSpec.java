/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.widget;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.litho.BoundaryWorkingRange;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.FocusedVisibleEvent;
import com.facebook.litho.FullImpressionVisibleEvent;
import com.facebook.litho.InvisibleEvent;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.VisibilityChangedEvent;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.events.EventWithoutAnnotation;

/**
 * LayoutSpec that implements lifecycle methods and crashes from the one provided as crashFromStep
 * prop.
 */
@LayoutSpec(events = EventWithoutAnnotation.class)
public class TestCrashFromEachLayoutLifecycleMethodSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c, StateValue<String> state, @Prop LifecycleStep crashFromStep) {
    state.set("hello world");
    if (crashFromStep == LifecycleStep.ON_CREATE_INITIAL_STATE) {
      throw new RuntimeException("onCreateInitialState crash");
    }
  }

  @OnCreateTreeProp
  static Rect onCreateTreePropRect(ComponentContext c, @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_CREATE_TREE_PROP) {
      throw new RuntimeException("onCreateTreeProp crash");
    }
    return new Rect();
  }

  @OnCalculateCachedValue(name = "expensiveValue")
  static int onCalculateExpensiveValue(@Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_CALCULATE_CACHED_VALUE) {
      throw new RuntimeException("onCalculateCachedValue crash");
    }
    return 0;
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop LifecycleStep crashFromStep,
      @Prop(optional = true) @Nullable Caller caller,
      @State String state,
      @CachedValue int expensiveValue) {
    if (state == null) {
      throw new IllegalStateException("OnCreateLayout called without initialised state.");
    }
    if (caller != null) {
      caller.set(c, crashFromStep);
    }
    if (crashFromStep == LifecycleStep.ON_CREATE_LAYOUT) {
      throw new RuntimeException("onCreateLayout crash");
    }
    return Column.create(c)
        .visibleHandler(TestCrashFromEachLayoutLifecycleMethod.onVisible(c))
        .invisibleHandler(TestCrashFromEachLayoutLifecycleMethod.onInvisible(c))
        .focusedHandler(TestCrashFromEachLayoutLifecycleMethod.onFocusedEventVisible(c))
        .fullImpressionHandler(
            TestCrashFromEachLayoutLifecycleMethod.onFullImpressionVisibleEvent(c))
        .visibilityChangedHandler(
            TestCrashFromEachLayoutLifecycleMethod.onVisibilityChangedEvent(c))
        .build();
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c, @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_CREATE_TRANSITION) {
      throw new RuntimeException("onCreateTransition crash");
    }
    return Transition.allLayout();
  }

  @OnAttached
  static void onAttached(ComponentContext c, @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_ATTACHED) {
      throw new RuntimeException("onAttached crash");
    }
  }

  @OnDetached
  static void onDetached(ComponentContext c, @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_DETACHED) {
      throw new RuntimeException("onDetached crash");
    }
  }

  @OnUpdateState
  static void updateState(@Param LifecycleStep crashFromStepAsParam) {
    if (crashFromStepAsParam == LifecycleStep.ON_UPDATE_STATE) {
      throw new RuntimeException("onUpdateState crash");
    }
  }

  @OnUpdateStateWithTransition
  static Transition updateStateWithTransition(@Param LifecycleStep crashFromStepAsParam) {
    if (crashFromStepAsParam == LifecycleStep.ON_UPDATE_STATE_WITH_TRANSITION) {
      throw new RuntimeException("onUpdateStateWithTransition crash");
    }
    return Transition.allLayout();
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(final ComponentContext c, final @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_EVENT_VISIBLE) {
      throw new RuntimeException("onEventVisible crash");
    }
  }

  @OnEvent(InvisibleEvent.class)
  static void onInvisible(final ComponentContext c, final @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_EVENT_INVISIBLE) {
      throw new RuntimeException("onEventInvisible crash");
    }
  }

  @OnEvent(FocusedVisibleEvent.class)
  static void onFocusedEventVisible(
      final ComponentContext c, final @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_FOCUSED_EVENT_VISIBLE) {
      throw new RuntimeException("onFocusedEventVisible crash");
    }
  }

  @OnEvent(FullImpressionVisibleEvent.class)
  static void onFullImpressionVisibleEvent(
      final ComponentContext c, final @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT) {
      throw new RuntimeException("onFullImpressionVisible crash");
    }
  }

  @OnEvent(VisibilityChangedEvent.class)
  static void onVisibilityChangedEvent(
      final ComponentContext c, final @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_VISIBILITY_CHANGED) {
      throw new RuntimeException("onVisibilityChanged crash");
    }
  }

  @OnRegisterRanges
  static void registerWorkingRanges(ComponentContext c, final @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_REGISTER_RANGES) {
      throw new RuntimeException("onRegisterRanges crash");
    }
    TestCrashFromEachLayoutLifecycleMethod.registerBoundaryWorkingRange(
        c, new BoundaryWorkingRange());
  }

  @OnEnteredRange(name = "boundary")
  static void onEnteredWorkingRange(ComponentContext c, @Prop LifecycleStep crashFromStep) {
    if (crashFromStep == LifecycleStep.ON_ENTERED_RANGE) {
      throw new RuntimeException("onEnteredRange crash");
    }
  }

  public static class Caller {

    ComponentContext c;
    LifecycleStep crashFromStep;

    void set(ComponentContext c, LifecycleStep crashFromStep) {
      this.c = c;
      this.crashFromStep = crashFromStep;
    }

    public void updateStateSync() {
      TestCrashFromEachLayoutLifecycleMethod.updateStateSync(c, crashFromStep);
    }

    public void updateStateWithTransition() {
      TestCrashFromEachLayoutLifecycleMethod.updateStateWithTransitionWithTransition(
          c, crashFromStep);
    }
  }
}
