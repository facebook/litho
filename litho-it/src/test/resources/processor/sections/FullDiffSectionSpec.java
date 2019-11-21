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
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.Diff;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.ChangesInfo;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnDataRendered;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.annotations.OnRefresh;
import com.facebook.litho.sections.annotations.OnUnbindService;
import com.facebook.litho.sections.annotations.OnViewportChanged;
import java.util.List;

@DiffSectionSpec(events = TestEvent.class)
public class FullDiffSectionSpec<T> implements TestTag {

  @OnCreateInitialState
  static void onCreateInitialState(
      SectionContext c, @Prop Integer prop1, StateValue<Object> state1) {}

  @OnDiff
  protected static <T> void onDiff(
      SectionContext c,
      ChangeSet changeSet,
      @Prop Diff<List<T>> data,
      @Prop Diff<Component> prop3,
      @State Diff<Object> state1) {}

  @OnUpdateState
  protected static void updateState(StateValue<Object> state1, @Param Object param) {}

  @OnCreateService
  static String onCreateService(SectionContext c, @Prop(optional = true) String prop2) {
    return prop2;
  }

  @OnBindService
  static void bindService(SectionContext c, String service) {}

  @OnUnbindService
  static void unbindService(SectionContext c, String service) {}

  @OnRefresh
  static void onRefresh(SectionContext c, String service) {}

  @OnDataBound
  static void onDataBound(SectionContext c) {}

  @ShouldUpdate
  static boolean shouldUpdate(@Prop Diff<Integer> prop1) {
    return true;
  }

  @OnViewportChanged
  static void onViewportChanged(
      SectionContext c,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int totalCount,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {}

  @OnEvent(ClickEvent.class)
  static void testEvent(SectionContext c, @FromEvent View view, @Param int someParam) {}

  @OnDataRendered
  static void onDataRendered(
      SectionContext c,
      boolean isDataChanged,
      boolean isMounted,
      long uptimeMillis,
      int firstVisibleIndex,
      int lastVisibleIndex,
      ChangesInfo changesInfo,
      int globalOffset,
      @Prop Integer prop1,
      @CachedValue int cached) {}

  @OnCalculateCachedValue(name = "cached")
  static int onCalculateCached(@Prop Integer prop1) {
    return 0;
  }
}
