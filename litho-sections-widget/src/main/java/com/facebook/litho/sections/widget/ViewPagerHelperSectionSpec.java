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

package com.facebook.litho.sections.widget;

import android.os.Handler;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.EventHandler;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionLifecycle;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnViewportChanged;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import java.util.List;
import javax.annotation.Nullable;

@GroupSectionSpec
@Nullsafe(value = Nullsafe.Mode.LOCAL)
class ViewPagerHelperSectionSpec<T> {

  @OnCreateInitialState
  static void onCreateInitialState(SectionContext c, StateValue<Integer> currentPageIndex) {
    currentPageIndex.set(-1);
  }

  @OnCreateChildren
  static <T> Children onCreateChildren(
      SectionContext c, @Prop List<T> data, @Prop EventHandler<RenderEvent> renderEventHandler) {
    return Children.create()
        .child(DataDiffSection.<T>create(c).data(data).renderEventHandler(renderEventHandler))
        .build();
  }

  @OnDataBound
  protected static void onDataBound(
      final SectionContext c,
      @Prop final int offset,
      @Prop(optional = true) final int initialPageIndex) {
    if (initialPageIndex >= 0) {
      new Handler()
          .post(
              new Runnable() {
                @Override
                public void run() {
                  SectionLifecycle.requestFocusWithOffset(c, initialPageIndex, offset);
                }
              });
    }
  }

  @OnViewportChanged
  static void onViewportChanged(
      SectionContext c,
      int firstVisiblePosition,
      int lastVisiblePosition,
      int totalCount,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex,
      @Prop(optional = true) @Nullable
          EventHandler<PageSelectedEvent> pageSelectedEventEventHandler,
      @State(canUpdateLazily = true) int currentPageIndex) {
    if (currentPageIndex == firstFullyVisibleIndex || firstFullyVisibleIndex < 0) {
      return;
    }
    ViewPagerHelperSection.lazyUpdateCurrentPageIndex(c, firstFullyVisibleIndex);
    if (pageSelectedEventEventHandler != null) {
      ViewPagerComponent.dispatchPageSelectedEvent(
          pageSelectedEventEventHandler, firstFullyVisibleIndex);
    }
  }
}
