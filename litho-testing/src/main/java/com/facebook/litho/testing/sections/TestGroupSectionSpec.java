/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.testing.litho.sections;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.OnCheckIsSameContentEvent;
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import java.util.Comparator;
import java.util.List;

@GroupSectionSpec
public class TestGroupSectionSpec {

  @OnCreateChildren
  protected static Children onCreateChildren(
      SectionContext c,
      @Prop List data,
      @Prop(optional = true) Comparator isSameItemComparator,
      @Prop(optional = true) Comparator isSameContentComparator) {

    DataDiffSection.Builder builder = DataDiffSection.create(c)
        .data(data)
        .renderEventHandler(TestGroupSection.onRender(c, c));

    if (isSameItemComparator != null) {
      builder.onCheckIsSameItemEventHandler(TestGroupSection.onCheckIsSameItem(c));
    }

    if (isSameContentComparator != null) {
      builder.onCheckIsSameContentEventHandler(TestGroupSection.onCheckIsSameContent(c));
    }

    return Children.create().child(builder.build()).build();
  }

  @OnEvent(RenderEvent.class)
  protected static RenderInfo onRender(
      SectionContext c,
      @FromEvent Object model,
      @Param ComponentContext context) {
    return ComponentRenderInfo.create()
        .component(Text.create(context).text(model.toString()).build())
        .build();
  }

  @OnEvent(OnCheckIsSameItemEvent.class)
  protected static boolean onCheckIsSameItem(
      SectionContext c,
      @FromEvent Object previousItem,
      @FromEvent Object nextItem,
      @Prop(optional = true) Comparator isSameItemComparator) {
    return isSameItemComparator.compare(previousItem, nextItem) == 0;
  }

  @OnEvent(OnCheckIsSameContentEvent.class)
  protected static boolean onCheckIsSameContent(
      SectionContext c,
      @FromEvent Object previousItem,
      @FromEvent Object nextItem,
      @Prop(optional = true) Comparator isSameContentComparator) {
    return isSameContentComparator.compare(previousItem, nextItem) == 0;
  }
}
