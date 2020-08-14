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

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.SnapUtil;

@Nullsafe(Nullsafe.Mode.LOCAL)
@LayoutSpec(events = PageSelectedEvent.class)
public class ViewPagerComponentSpec<T> {

  @OnCreateLayout
  static <T> Component onCreateLayout(
      ComponentContext c,
      @Prop DataDiffSection<T> dataDiffSection,
      @Prop RecyclerCollectionEventsController eventsController,
      @Prop(optional = true) int initialPageIndex) {
    final RecyclerConfiguration recyclerConfiguration =
        ListRecyclerConfiguration.create()
            .orientation(LinearLayoutManager.HORIZONTAL)
            .snapMode(SnapUtil.SNAP_TO_CENTER)
            .linearLayoutInfoFactory(
                new LinearLayoutInfoFactory() {
                  @Override
                  public LinearLayoutInfo createLinearLayoutInfo(
                      Context context, int orientation, boolean reverseLayout) {
                    return new ViewPagerLinearLayoutInfo(context, orientation, reverseLayout);
                  }
                })
            .build();

    return RecyclerCollectionComponent.create(c)
        .flexGrow(1)
        .disablePTR(true)
        .section(
            ViewPagerHelperSection.<T>create(new SectionContext(c))
                .delegateSection(dataDiffSection)
                .pageSelectedEventEventHandler(ViewPagerComponent.getPageSelectedEventHandler(c))
                .initialPageIndex(initialPageIndex))
        .eventsController(eventsController)
        .recyclerConfiguration(recyclerConfiguration)
        .build();
  }

  /** Custom implementation of LinearLayout to assign parent's width to items. */
  private static class ViewPagerLinearLayoutInfo extends LinearLayoutInfo {

    public ViewPagerLinearLayoutInfo(Context context, int orientation, boolean reverseLayout) {
      super(context, orientation, reverseLayout);
    }

    @Override
    public int getChildWidthSpec(int widthSpec, RenderInfo renderInfo) {
      final int hscrollWidth = SizeSpec.getSize(widthSpec);
      return SizeSpec.makeSizeSpec(hscrollWidth, SizeSpec.EXACTLY);
    }
  }
}
