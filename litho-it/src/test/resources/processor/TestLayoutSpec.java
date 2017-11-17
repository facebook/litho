/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor.integration.resources;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.Output;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.TransitionSet;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;

@LayoutSpec(events = TestEvent.class)
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class TestLayoutSpec<S extends View> {
  @PropDefault protected static final boolean prop2 = true;

  @OnLoadStyle
  static void onLoadStyle(
      ComponentContext c,
      Output<Boolean> prop2,
      Output<Object> prop3) {
  }

  @OnCreateInitialState
  static <S extends View> void createInitialState(
      ComponentContext c,
      @Prop int prop1,
      StateValue<S> state2) {
  }

  @OnCreateTreeProp
  static TestTreeProp onCreateFeedPrefetcherProp(
      ComponentContext c,
      @Prop long prop6) {
    return new TestTreeProp(prop6);
  }

  @OnCreateLayout
  static <S extends View> ComponentLayout onCreateLayout(
      ComponentContext context,
      @Prop(optional = true) boolean prop2,
      @Prop Object prop3,
      @Prop char[] prop4,
      @State(canUpdateLazily = true) long state1,
      @State S state2,
      @State int state3,
      @TreeProp TestTreeProp treeProp,
      @Prop Component<?> child) {
    return null;
  }

  @OnEvent(ClickEvent.class)
  static void testLayoutEvent(
      ComponentContext c,
      @FromEvent View view,
      @Param int param1,
      @Prop Object prop3,
      @Prop char prop5,
      @State(canUpdateLazily = true) long state1) {
  }

  @OnUpdateState
  static void updateCurrentState(
      StateValue<Long> state1,
      @Param int someParam) {
  }

  @OnCreateTransition
  static Transition onCreateTransition(
      ComponentContext c,
      @Prop Object prop3,
      @State(canUpdateLazily = true) long state1,
      @State Diff<Integer> state3) {
    return Transition.parallel(
      Transition.create("testKey"));
  }
}
