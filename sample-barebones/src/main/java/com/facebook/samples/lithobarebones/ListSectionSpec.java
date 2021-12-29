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

package com.facebook.samples.lithobarebones;

import static com.facebook.litho.widget.SnapUtil.SNAP_TO_CENTER;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.viewcompat.SimpleViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.ViewRenderInfo;
import java.util.ArrayList;
import java.util.List;

@GroupSectionSpec
class ListSectionSpec {

  // It's important for the view creator to be defined only once as the recycling for views is
  // driven by the instance
  // of the view creator that was used to create them.
  private static final ViewCreator VIEW_CREATOR =
      new ViewCreator() {
        @Override
        public View createView(Context c, ViewGroup parent) {
          return LayoutInflater.from(c).inflate(R.layout.simple_view, null);
        }
      };

  @OnCreateChildren
  static Children onCreateChildren(final SectionContext c) {
    return Children.create()
        .child(
            SingleComponentSection.create(c)
                .component(
                    RecyclerCollectionComponent.create(c)
                        .disablePTR(true)
                        .recyclerConfiguration(
                            new ListRecyclerConfiguration(
                                LinearLayoutManager.HORIZONTAL, false, SNAP_TO_CENTER))
                        .section(
                            DataDiffSection.<Integer>create(c)
                                .data(generateData(32))
                                .renderEventHandler(ListSection.onRender(c))
                                .build())
                        .canMeasureRecycler(true))
                .build())
        .child(
            DataDiffSection.<Integer>create(c)
                .data(generateData(32))
                .renderEventHandler(ListSection.onRender(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(final SectionContext c, @FromEvent Integer model) {
    if (model.intValue() == 1) {
      return ViewRenderInfo.create()
          .viewBinder(
              new SimpleViewBinder<TextView>() {
                @Override
                public void bind(TextView textView) {
                  textView.setText("I'm a view in a Litho world");
                }
              })
          .viewCreator(VIEW_CREATOR)
          .build();
    }

    return ComponentRenderInfo.create()
        .component(
            ListItem.create(c)
                .color(model % 2 == 0 ? Color.WHITE : Color.LTGRAY)
                .title(model + ". Hello, world!")
                .subtitle("Litho tutorial")
                .build())
        .build();
  }

  private static List<Integer> generateData(int count) {
    final List<Integer> data = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      data.add(i);
    }
    return data;
  }
}
