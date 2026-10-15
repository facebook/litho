/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.samples.litho;

import static com.facebook.litho.core.CoreStylesKt.widthPercent;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView.ScaleType;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.JavaStyle;
import com.facebook.litho.Style;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.PrimitiveImage;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.TouchableFeedback;
import com.facebook.yoga.YogaEdge;
import java.util.Arrays;
import java.util.List;

@GroupSectionSpec
class DemoListSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c, @Prop List<Demos.DemoGrouping> dataModels) {
    final Children.Builder children = Children.create();
    for (int i = 0; i < dataModels.size(); i++) {
      final Demos.DemoGrouping model = dataModels.get(i);
      children.child(
          SingleComponentSection.create(c)
              .component(DemoGroupTitleComponent.create(c).title(model.getName()))
              .sticky(true));
      children.child(
          DataDiffSection.<Demos.NavigableDemoItem>create(c)
              .data(model.getDatamodels())
              .renderEventHandler(
                  DemoListSection.onRenderComponentDemo(
                      c, i, dataModels.get(i).getDatamodels().size())));
    }
    return children.build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRenderComponentDemo(
      SectionContext c,
      @Prop int[] previousIndices,
      @FromEvent int index,
      @FromEvent Demos.NavigableDemoItem model,
      @Param int groupIndex,
      @Param int groupSize) {
    int[] newIndices = Arrays.copyOf(previousIndices, previousIndices.length + 2);
    newIndices[newIndices.length - 2] = groupIndex;
    newIndices[newIndices.length - 1] = index;
    return ComponentRenderInfo.create()
        .component(
            new TouchableFeedback(
                Column.create(c)
                    .child(SingleDemoComponent.create(c).title(model.getName()))
                    .child(
                        index != groupSize - 1
                            ? PrimitiveImage.create(
                                new ColorDrawable(Color.BLACK),
                                ScaleType.FIT_CENTER,
                                true,
                                null,
                                1,
                                new JavaStyle(widthPercent(Style.Companion, 100f))
                                    .paddingDip(YogaEdge.LEFT, 16f)
                                    .build())
                            : null)
                    .clickHandler(DemoListSection.onClickEvent(c, newIndices, model))
                    .build(),
                Color.WHITE,
                Color.LTGRAY))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(
      SectionContext c, @Param int[] indices, @Param Demos.NavigableDemoItem model) {
    c.getAndroidContext().startActivity(model.getIntent(c.getAndroidContext(), indices));
  }
}
