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

import static com.facebook.litho.annotations.ResType.STRING;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.DefaultMountContentPool;
import com.facebook.litho.Diff;
import com.facebook.litho.MountContentPool;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.GetExtraAccessibilityNodeAt;
import com.facebook.litho.annotations.GetExtraAccessibilityNodesCount;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnCreateMountContentPool;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPopulateExtraAccessibilityNode;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import javax.annotation.Nullable;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
@MountSpec(
    events = TestEvent.class,
    isPureRender = true,
    hasChildLithoViews = true,
    canPreallocate = true)
public class TestMountSpec<S extends View> implements TestTag {
  @PropDefault protected static final boolean prop2 = true;

  @OnLoadStyle
  static void onLoadStyle(ComponentContext c, Output<Boolean> prop2, Output<Object> prop3) {}

  @OnCreateInitialState
  static <S extends View> void createInitialState(
      ComponentContext c, @Prop int prop1, StateValue<S> state2) {}

  @OnCreateTreeProp
  static TestTreeProp onCreateFeedPrefetcherProp(ComponentContext c, @Prop long prop6) {
    return new TestTreeProp(prop6);
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      Output<Long> measureOutput) {}

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      @Prop Object prop3,
      @Prop char[] prop4,
      @FromMeasure Long measureOutput,
      Output<Integer> boundsDefinedOutput) {}

  @OnCreateMountContent
  static Drawable onCreateMountContent(Context c) {
    return new ColorDrawable(Color.RED);
  }

  @OnMount
  static <S extends View> void onMount(
      ComponentContext c,
      Drawable v,
      @Prop(optional = true) boolean prop2,
      @State(canUpdateLazily = true) long state1,
      @State S state2,
      @FromMeasure Long measureOutput,
      @TreeProp TestTreeProp treeProp) {}

  @OnUnmount
  static void onUnmount(ComponentContext c, Drawable v, @Prop long prop8) {}

  @OnPopulateAccessibilityNode
  static void onPopulateAccessibilityNode(
      ComponentContext c,
      View host,
      AccessibilityNodeInfoCompat node,
      @Prop(resType = STRING) @Nullable CharSequence prop7) {}

  @GetExtraAccessibilityNodesCount
  static int getExtraAccessibilityNodesCount(
      ComponentContext c,
      @Prop int prop1,
      @Prop(resType = STRING) @Nullable CharSequence prop7,
      @FromBoundsDefined Integer boundsDefinedOutput) {
    return 1;
  }

  @OnPopulateExtraAccessibilityNode
  static void onPopulateExtraAccessibilityNode(
      ComponentContext c,
      AccessibilityNodeInfoCompat node,
      int extraNodeIndex,
      int componentBoundsLeft,
      int componentBoundsTop,
      @Prop Object prop3,
      @Prop(resType = STRING) @Nullable CharSequence prop7,
      // @CachedValue int cached, // It seems the A11Y APIs does not accept ComponentContext
      @FromBoundsDefined Integer boundsDefinedOutput) {}

  @GetExtraAccessibilityNodeAt
  static int getExtraAccessibilityNodeAt(
      ComponentContext c,
      int x,
      int y,
      @Prop Object prop3,
      @Prop(resType = STRING) @Nullable CharSequence prop7,
      @FromBoundsDefined Integer boundsDefinedOutput) {
    return 0;
  }

  @OnEvent(ClickEvent.class)
  static void testLayoutEvent(
      ComponentContext c,
      @Prop Object prop3,
      @Prop char prop5,
      @FromEvent View view,
      @Param int param1,
      @State(canUpdateLazily = true) long state1,
      @CachedValue int cached) {}

  @OnTrigger(ClickEvent.class)
  static void onClickEventTrigger(ComponentContext c, @FromTrigger View view, @Prop Object prop3) {}

  @OnUpdateState
  static void updateCurrentState(StateValue<Long> state1, @Param int someParam) {}

  @OnCreateTransition
  static Transition onCreateTransition(
      ComponentContext c, @Prop Object prop3, @State(canUpdateLazily = true) long state1) {
    return Transition.parallel(Transition.create(Transition.TransitionKeyType.GLOBAL, "testKey"));
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(@Prop Diff<Integer> prop1) {
    return true;
  }

  @OnCreateMountContentPool
  static MountContentPool onCreateMountContentPool() {
    return new DefaultMountContentPool("MyCustomPool", 3, true);
  }

  @OnCalculateCachedValue(name = "cached")
  static int onCalculateCached(
      @Prop Object prop3, @Prop char prop5, @State(canUpdateLazily = true) long state1) {
    return 0;
  }
}
