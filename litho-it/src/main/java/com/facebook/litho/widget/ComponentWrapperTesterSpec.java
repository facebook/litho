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

package com.facebook.litho.widget;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.FocusedVisibleEvent;
import com.facebook.litho.FullImpressionVisibleEvent;
import com.facebook.litho.InvisibleEvent;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.LifecycleTracker;
import com.facebook.litho.UnfocusedVisibleEvent;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.Wrapper;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;

@LayoutSpec
public class ComponentWrapperTesterSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context, @Prop com.facebook.litho.LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_CREATE_INITIAL_STATE);
  }

  @OnCreateLayout
  protected static Component onCreateLayout(
      ComponentContext c, @Prop Component content, @Prop LifecycleTracker lifecycleTracker) {
    return Wrapper.create(c)
        .delegate(content)
        .visibleHandler(ComponentWrapperTester.onVisible(c))
        .focusedHandler(ComponentWrapperTester.onFocusedVisible(c))
        .invisibleHandler(ComponentWrapperTester.onInvisible(c))
        .unfocusedHandler(ComponentWrapperTester.onUnfocusedVisibleEvent(c))
        .fullImpressionHandler(ComponentWrapperTester.onFullImpressionVisibleEvent(c))
        .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(ComponentContext c, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_EVENT_VISIBLE);
  }

  @OnEvent(FocusedVisibleEvent.class)
  static void onFocusedVisible(ComponentContext c, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE);
  }

  @OnEvent(InvisibleEvent.class)
  static void onInvisible(ComponentContext c, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_EVENT_INVISIBLE);
  }

  @OnEvent(UnfocusedVisibleEvent.class)
  static void onUnfocusedVisibleEvent(ComponentContext c, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE);
  }

  @OnEvent(FullImpressionVisibleEvent.class)
  static void onFullImpressionVisibleEvent(
      ComponentContext c, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT);
  }
}
