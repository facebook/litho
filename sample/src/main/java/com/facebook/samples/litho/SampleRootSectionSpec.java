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

import android.text.Layout;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
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
import com.facebook.litho.widget.Card;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.TouchableFeedback;
import com.facebook.yoga.YogaEdge;
import java.util.List;

@GroupSectionSpec
class SampleRootSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c, @Prop List<Demos.DemoList> dataModels) {
    return Children.create()
        .child(
            DataDiffSection.<Demos.DemoList>create(c)
                .data(dataModels)
                .renderEventHandler(SampleRootSection.onRender(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(
      SectionContext c, @FromEvent int index, @FromEvent Demos.DemoList model) {
    return ComponentRenderInfo.create()
        .component(
            Column.create(c)
                .child(
                    Card.create(c)
                        .content(
                            TouchableFeedback.create(c)
                                .content(
                                    Text.create(c)
                                        .text(model.getName())
                                        .textSizeSp(22)
                                        .textAlignment(Layout.Alignment.ALIGN_CENTER)
                                        .paddingDip(YogaEdge.VERTICAL, 20)
                                        .clickHandler(
                                            SampleRootSection.onClickEvent(c, model, index))))
                        .marginDip(YogaEdge.HORIZONTAL, 16)
                        .marginDip(YogaEdge.TOP, 16)))
        .build();
  }

  // An event handler SampleRootSection.onClickEvent(c)
  @OnEvent(ClickEvent.class)
  static void onClickEvent(
      SectionContext c, @Param Demos.NavigableDemoItem model, @Param int index) {
    c.getAndroidContext().startActivity(model.getIntent(c.getAndroidContext(), new int[] {index}));
  }
}
