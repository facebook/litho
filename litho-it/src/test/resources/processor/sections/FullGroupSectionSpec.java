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

package com.facebook.litho.sections.processor.integration.resources;

import android.view.View;
import android.widget.TextView;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.Diff;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.sections.ChangesInfo;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnDataRendered;
import com.facebook.litho.sections.annotations.OnRefresh;
import com.facebook.litho.sections.annotations.OnUnbindService;
import com.facebook.litho.sections.annotations.OnViewportChanged;

/** Comment to be copied in generated section */
@GroupSectionSpec(events = TestEvent.class, isPublic = false)
public class FullGroupSectionSpec<T> implements TestTag {

  static class TreePropWrapper {}

  @OnCreateInitialState
  static <T> void onCreateInitialState(
      SectionContext c, @Prop int prop1, StateValue<T> state1, StateValue<Object> state2) {}

  @OnCreateTreeProp
  static TreePropWrapper onCreateTreeProp(SectionContext c, @TreeProp TreePropWrapper treeProp) {
    return new TreePropWrapper();
  }

  @OnCreateService
  static String onCreateService(SectionContext c, @Prop(optional = true) String prop2) {
    return prop2;
  }

  @OnCreateChildren
  protected static <T> Children onCreateChildren(
      SectionContext c,
      @Prop Component prop3,
      @Prop(resType = ResType.STRING) String prop4,
      @Prop Section prop5,
      @State T state1) {
    return null;
  }

  @OnUpdateState
  protected static void updateState(StateValue<Object> state2, @Param Object param) {}

  @OnBindService
  static void bindService(
      SectionContext c,
      String service,
      @Prop int prop1,
      @State(canUpdateLazily = true) Object state2) {}

  @OnUnbindService
  static void unbindService(
      SectionContext c,
      String service,
      @Prop int prop1,
      @State(canUpdateLazily = true) Object state2) {}

  @OnRefresh
  static void onRefresh(SectionContext c, String service, @Prop(optional = true) String prop2) {}

  @OnDataBound
  static void onDataBound(
      SectionContext c, @Prop Component prop3, @State(canUpdateLazily = true) Object state2) {}

  @ShouldUpdate
  static boolean shouldUpdate(@Prop Diff<Integer> prop1) {
    return true;
  }

  @OnViewportChanged
  static <T> void onViewportChanged(
      SectionContext c,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int totalCount,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex,
      @State T state1,
      @State(canUpdateLazily = true) Object state2,
      @Prop int prop1,
      @Prop(optional = true) String prop2,
      @Prop Component prop3) {}

  @OnEvent(ClickEvent.class)
  static void testEvent(
      SectionContext c,
      @FromEvent(baseClass = View.class) TextView view,
      @Param int someParam,
      @State(canUpdateLazily = true) Object state2,
      @Prop(optional = true) String prop2) {}

  @OnDataRendered
  static void onDataRendered(
      SectionContext c,
      boolean isDataChanged,
      boolean isMounted,
      long uptimeMillis,
      int firstVisibleIndex,
      int lastVisibleIndex,
      ChangesInfo changesInfo,
      @Prop int prop1,
      @State(canUpdateLazily = true) Object state2,
      @CachedValue int cached) {}

  @OnCalculateCachedValue(name = "cached")
  static int onCalculateCached(@Prop int prop1) {
    return 0;
  }
}
