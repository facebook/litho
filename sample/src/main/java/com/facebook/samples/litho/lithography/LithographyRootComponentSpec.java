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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.widget.LithoViewFactory;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.yoga.YogaEdge;
import java.util.List;

@LayoutSpec
public class LithographyRootComponentSpec {

  private static final String MAIN_SCREEN = "main_screen";

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop List<Datum> dataModels) {

    return RecyclerCollectionComponent.create(c)
            .disablePTR(true)
            .recyclerConfiguration(
                    ListRecyclerConfiguration.create()
                            .recyclerBinderConfiguration(
                                    RecyclerBinderConfiguration.create()
                                            .lithoViewFactory(
                                                    new LithoViewFactory() {
                                                      @Override
                                                      public LithoView createLithoView(ComponentContext context) {
                                                        return new LithoView(
                                                                context,
                                                                null,
                                                                false,
                                                                false,
                                                                new LithoView.VisiblityExtensionProvider() {
                                                                  @Override
                                                                  public MountExtension getVisibilityExtension(
                                                                          LithoView lithoView) {
                                                                    return new CustomVisibilityExtension(lithoView);
                                                                  }
                                                                });
                                                      }
                                                    })
                                            .build())
                            .build())
            .section(
                    DataDiffSection.<Datum>create(new SectionContext(c))
                            .data(dataModels)
                            .renderEventHandler(
                                    com.facebook.samples.litho.lithography.LithographyRootComponent.onRender(c))
                            .build())
            .paddingDip(YogaEdge.TOP, 8)
            .testKey(MAIN_SCREEN)
            .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(ComponentContext c, @FromEvent Datum model) {
    return model.createComponent(c);
  }
}
