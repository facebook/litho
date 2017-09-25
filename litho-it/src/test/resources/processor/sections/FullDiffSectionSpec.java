/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.integration.resources;

import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.Diff;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnDestroyService;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.annotations.OnRefresh;
import com.facebook.litho.sections.annotations.OnUnbindService;
import com.facebook.litho.sections.annotations.OnViewportChanged;
import java.util.List;

@DiffSectionSpec(events = TestEvent.class)
public class FullDiffSectionSpec<T> {

  @OnCreateInitialState
  static void onCreateInitialState(
      SectionContext c,
      @Prop Integer prop1,
      StateValue<Object> state1) {}

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
  static String onCreateService(
      SectionContext c,
      @Prop(optional = true) String prop2) {
    return prop2;
  }

  @OnBindService
  static void bindService(
      SectionContext c,
      String service) {

  }

  @OnUnbindService
  static void unbindService(
      SectionContext c,
      String service) {

  }

  @OnDestroyService
  static void destroyService(
      SectionContext c,
      String service) {

  }

  @OnRefresh
  static void onRefresh(
      SectionContext c,
      String service) {

  }

  @OnDataBound
  static void onDataBound(
      SectionContext c) {

  }

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
      int lastFullyVisibleIndex) {

  }

  @OnEvent(ClickEvent.class)
  static void testEvent(SectionContext c, @FromEvent View view, @Param int someParam) {}
}
