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

package com.facebook.litho.processor.integration.resources;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Diff;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Output;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnError;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import java.util.ArrayList;
import java.util.List;

@LayoutSpec(events = TestEvent.class, simpleNameDelegate = "child")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class TestLayoutSpec<S extends View> implements TestTag {
  @PropDefault protected static final boolean prop2 = true;
  @PropDefault protected static final List<String> names = new ArrayList<>();

  @OnLoadStyle
  static void onLoadStyle(ComponentContext c, Output<Boolean> prop2, Output<Object> prop3) {}

  @OnCreateInitialState
  static <S extends View> void createInitialState(
      ComponentContext c, @Prop int prop1, StateValue<S> state2) {}

  @OnCreateTreeProp
  static TestTreeProp onCreateFeedPrefetcherProp(ComponentContext c, @Prop long prop6) {
    return new TestTreeProp(prop6);
  }

  @OnCreateLayout
  static <S extends View> Component onCreateLayout(
      ComponentContext context,
      @Prop @Nullable Object prop3,
      @Prop char[] prop4,
      @Prop EventHandler<ClickEvent> handler,
      @Prop Component child,
      @Prop(optional = true) boolean prop2,
      @Prop(resType = ResType.STRING, optional = true, varArg = "name") List<String> names,
      @State(canUpdateLazily = true) long state1,
      @State S state2,
      @State int state3,
      @TreeProp TestTreeProp treeProp,
      @CachedValue int cached) {
    return null;
  }

  @OnEvent(ClickEvent.class)
  static void testLayoutEvent(
      ComponentContext c,
      @FromEvent View view,
      @Param int param1,
      @Prop @Nullable Object prop3,
      @Prop char prop5,
      @Prop(isCommonProp = true) float aspectRatio,
      @Prop(isCommonProp = true, overrideCommonPropBehavior = true) boolean focusable,
      @State(canUpdateLazily = true) long state1) {}

  @OnError
  static void onError(ComponentContext c, Exception e) {
    throw new RuntimeException(e);
  }

  @OnTrigger(ClickEvent.class)
  static void onClickEventTrigger(ComponentContext c, @FromTrigger View view) {}

  @OnUpdateState
  static void updateCurrentState(StateValue<Long> state1, @Param int someParam) {}

  @OnCreateTransition
  static Transition onCreateTransition(
      ComponentContext c,
      @Prop @Nullable Object prop3,
      @State(canUpdateLazily = true) long state1,
      @State Diff<Integer> state3) {
    return Transition.parallel(Transition.create(Transition.TransitionKeyType.GLOBAL, "testKey"));
  }

  @OnCalculateCachedValue(name = "cached")
  static int onCalculateCached(
      @Prop @Nullable Object prop3, @Prop char prop5, @State(canUpdateLazily = true) long state1) {
    return 0;
  }
}
