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

package com.facebook.samples.litho.lithography;

import static androidx.recyclerview.widget.LinearSmoothScroller.SNAP_TO_START;

import androidx.recyclerview.widget.LinearLayoutManager;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.fresco.FrescoImage;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.sections.widget.RecyclerConfiguration;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.Arrays;

@LayoutSpec
public class FeedImageComponentSpec {

  private static final RecyclerConfiguration LIST_CONFIGURATION =
      new ListRecyclerConfiguration(
          LinearLayoutManager.HORIZONTAL, /*reverseLayout*/ false, SNAP_TO_START);

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop final String[] images) {
    return images.length == 1
        ? createImageComponent(c, images[0]).build()
        : RecyclerCollectionComponent.create(c)
            .disablePTR(true)
            .recyclerConfiguration(LIST_CONFIGURATION)
            .section(
                DataDiffSection.<String>create(new SectionContext(c))
                    .data(Arrays.asList(images))
                    .renderEventHandler(FeedImageComponent.onRender(c))
                    .build())
            .canMeasureRecycler(true)
            .aspectRatio(2)
            .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(ComponentContext c, @FromEvent String model) {
    return ComponentRenderInfo.create().component(createImageComponent(c, model).build()).build();
  }

  private static Component.Builder createImageComponent(ComponentContext c, String image) {
    final DraweeController controller = Fresco.newDraweeControllerBuilder().setUri(image).build();

    return FrescoImage.create(c).controller(controller).imageAspectRatio(2f);
  }
}
