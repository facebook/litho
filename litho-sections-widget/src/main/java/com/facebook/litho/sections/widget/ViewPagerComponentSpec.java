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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.SnapUtil;
import java.util.List;

@Nullsafe(Nullsafe.Mode.LOCAL)
@LayoutSpec(events = PageSelectedEvent.class)
public class ViewPagerComponentSpec<T> {

  @OnCreateLayout
  static <T> Component onCreateLayout(
      ComponentContext c,
      @Prop final EventHandler<RenderEvent> renderEventHandler,
      @Prop final List<T> data,
      @Prop RecyclerCollectionEventsController eventsController,
      @Prop(optional = true) final int itemSpacingDp,
      @Prop(optional = true) int initialPageIndex) {

    final int spacingInPixels = c.getResourceResolver().dipsToPixels(itemSpacingDp);

    final RecyclerConfiguration recyclerConfiguration =
        ListRecyclerConfiguration.create()
            .orientation(LinearLayoutManager.HORIZONTAL)
            .snapMode(SnapUtil.SNAP_TO_CENTER)
            .linearLayoutInfoFactory(
                new LinearLayoutInfoFactory() {
                  @Override
                  public LinearLayoutInfo createLinearLayoutInfo(
                      Context context, int orientation, boolean reverseLayout) {
                    return new ViewPagerLinearLayoutInfo(
                        context, orientation, reverseLayout, spacingInPixels, data.size() > 1);
                  }
                })
            .build();

    final RecyclerCollectionComponent.Builder builder =
        RecyclerCollectionComponent.create(c)
            .disablePTR(true)
            .section(
                ViewPagerHelperSection.<T>create(new SectionContext(c))
                    .data(data)
                    .renderEventHandler(renderEventHandler)
                    .pageSelectedEventEventHandler(
                        ViewPagerComponent.getPageSelectedEventHandler(c))
                    .initialPageIndex(initialPageIndex)
                    .offset((int) (spacingInPixels * 3f)))
            .eventsController(eventsController)
            .recyclerConfiguration(recyclerConfiguration);
    if (itemSpacingDp > 0) {
      builder.itemDecoration(new ViewPagerItemDecoration(spacingInPixels, data.size()));
    }
    return builder.build();
  }

  /** Custom implementation of LinearLayout to assign parent's width to items. */
  private static class ViewPagerLinearLayoutInfo extends LinearLayoutInfo {
    private final int spacingInPixels;
    private final boolean adjustWidthForSpacing;

    public ViewPagerLinearLayoutInfo(
        Context context,
        int orientation,
        boolean reverseLayout,
        int spacingInPixels,
        boolean adjustWidthForSpacing) {
      super(context, orientation, reverseLayout);
      this.spacingInPixels = spacingInPixels;
      this.adjustWidthForSpacing = adjustWidthForSpacing;
    }

    @Override
    public int getChildWidthSpec(int widthSpec, RenderInfo renderInfo) {
      final int hscrollWidth = SizeSpec.getSize(widthSpec);
      return SizeSpec.makeSizeSpec(
          hscrollWidth - (adjustWidthForSpacing ? 7 * spacingInPixels : 2 * spacingInPixels),
          SizeSpec.EXACTLY);
    }
  }

  private static final class ViewPagerItemDecoration extends RecyclerView.ItemDecoration {
    private final int itemSpacing;
    private final int itemsCount;

    public ViewPagerItemDecoration(int itemSpacing, int itemsCount) {
      this.itemSpacing = itemSpacing;
      this.itemsCount = itemsCount;
    }

    @Override
    @TargetApi(17)
    public void getItemOffsets(
        Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
      final int index = parent.getChildAdapterPosition(view);
      int leftPadding = index == 0 ? itemSpacing : itemSpacing / 2;
      int rightPadding = index == itemsCount - 1 ? itemSpacing : itemSpacing / 2;
      outRect.set(leftPadding, 0, rightPadding, 0);
    }
  }
}
