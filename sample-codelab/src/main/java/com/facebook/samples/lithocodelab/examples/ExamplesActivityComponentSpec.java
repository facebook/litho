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

package com.facebook.samples.lithocodelab.examples;

import androidx.recyclerview.widget.OrientationHelper;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.samples.lithocodelab.examples.modules.LearningClickEventsComponent;
import com.facebook.samples.lithocodelab.examples.modules.LearningClickEventsComponentSpec;
import com.facebook.samples.lithocodelab.examples.modules.LearningContainersComponent;
import com.facebook.samples.lithocodelab.examples.modules.LearningContainersComponentSpec;
import com.facebook.samples.lithocodelab.examples.modules.LearningLayoutPropsComponent;
import com.facebook.samples.lithocodelab.examples.modules.LearningLayoutPropsComponentSpec;
import com.facebook.samples.lithocodelab.examples.modules.LearningLayoutSpecsComponent;
import com.facebook.samples.lithocodelab.examples.modules.LearningLayoutSpecsComponentSpec;
import com.facebook.samples.lithocodelab.examples.modules.LearningPropsComponent;
import com.facebook.samples.lithocodelab.examples.modules.LearningPropsComponentSpec;
import com.facebook.samples.lithocodelab.examples.modules.LearningRecyclerBinderComponent;
import com.facebook.samples.lithocodelab.examples.modules.LearningRecyclerBinderComponentSpec;
import com.facebook.samples.lithocodelab.examples.modules.LearningStateComponent;
import com.facebook.samples.lithocodelab.examples.modules.LearningTextWidgetComponent;
import com.facebook.samples.lithocodelab.examples.modules.LearningTextWidgetComponentSpec;

/**
 * Intended order of learning: {@link LearningLayoutSpecsComponentSpec} {@link
 * LearningTextWidgetComponentSpec} {@link LearningContainersComponentSpec} {@link
 * LearningPropsComponentSpec} {@link LearningLayoutPropsComponentSpec} {@link
 * LearningClickEventsComponentSpec} {@link LearningRecyclerBinderComponentSpec}
 */
@LayoutSpec
class ExamplesActivityComponentSpec {

  private static class Populator {
    final RecyclerBinder recyclerBinder;
    final ComponentContext c;

    int position = 0;

    private Populator(RecyclerBinder recyclerBinder, ComponentContext c) {
      this.recyclerBinder = recyclerBinder;
      this.c = c;
    }

    private static Populator with(RecyclerBinder recyclerBinder, ComponentContext c) {
      return new Populator(recyclerBinder, c);
    }

    private Populator addRow(String renderText, EventHandler<ClickEvent> clickEventHandler) {
      recyclerBinder.insertItemAt(
          position,
          ExamplesRowComponent.create(c)
              .text(renderText)
              .clickEventHandler(clickEventHandler)
              .build());
      position++;

      return this;
    }
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(new LinearLayoutInfo(c, OrientationHelper.VERTICAL, false))
            .build(c);

    Populator.with(recyclerBinder, c)
        .addRow("Layout Specs", ExamplesActivityComponent.onClickLayoutSpecs(c))
        .addRow("Text Widget", ExamplesActivityComponent.onClickTextWidget(c))
        .addRow("Containers", ExamplesActivityComponent.onClickContainers(c))
        .addRow("Props", ExamplesActivityComponent.onClickProps(c))
        .addRow("Layout Props", ExamplesActivityComponent.onClickLayoutProps(c))
        .addRow("Click Events", ExamplesActivityComponent.onClickClickEvents(c))
        .addRow("State", ExamplesActivityComponent.onClickState(c))
        .addRow("Recycler Binder", ExamplesActivityComponent.onClickRecyclerBinder(c));

    return Recycler.create(c).binder(recyclerBinder).build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickLayoutSpecs(
      ComponentContext c,
      @Prop ExamplesLithoLabActivity.LabExampleController labExampleController) {
    labExampleController.setContentComponent(LearningLayoutSpecsComponent.create(c).build());
  }

  @OnEvent(ClickEvent.class)
  static void onClickTextWidget(
      ComponentContext c,
      @Prop ExamplesLithoLabActivity.LabExampleController labExampleController) {
    labExampleController.setContentComponent(LearningTextWidgetComponent.create(c).build());
  }

  @OnEvent(ClickEvent.class)
  static void onClickContainers(
      ComponentContext c,
      @Prop ExamplesLithoLabActivity.LabExampleController labExampleController) {
    labExampleController.setContentComponent(LearningContainersComponent.create(c).build());
  }

  @OnEvent(ClickEvent.class)
  static void onClickProps(
      ComponentContext c,
      @Prop ExamplesLithoLabActivity.LabExampleController labExampleController) {
    labExampleController.setContentComponent(
        LearningPropsComponent.create(c).text1("Props, world!").text2("World, props!").build());
  }

  @OnEvent(ClickEvent.class)
  static void onClickLayoutProps(
      ComponentContext c,
      @Prop ExamplesLithoLabActivity.LabExampleController labExampleController) {
    labExampleController.setContentComponent(LearningLayoutPropsComponent.create(c).build());
  }

  @OnEvent(ClickEvent.class)
  static void onClickClickEvents(
      ComponentContext c,
      @Prop ExamplesLithoLabActivity.LabExampleController labExampleController) {
    labExampleController.setContentComponent(
        LearningClickEventsComponent.create(c)
            .secondChildString("Prop passed in from parent.")
            .build());
  }

  @OnEvent(ClickEvent.class)
  static void onClickState(
      ComponentContext c,
      @Prop ExamplesLithoLabActivity.LabExampleController labExampleController) {
    labExampleController.setContentComponent(LearningStateComponent.create(c).build());
  }

  @OnEvent(ClickEvent.class)
  static void onClickRecyclerBinder(
      ComponentContext c,
      @Prop ExamplesLithoLabActivity.LabExampleController labExampleController) {
    labExampleController.setContentComponent(LearningRecyclerBinderComponent.create(c).build());
  }
}
