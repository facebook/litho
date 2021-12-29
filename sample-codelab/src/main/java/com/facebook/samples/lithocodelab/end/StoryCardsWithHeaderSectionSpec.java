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

package com.facebook.samples.lithocodelab.end;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import java.util.ArrayList;
import java.util.List;

@GroupSectionSpec
class StoryCardsWithHeaderSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c) {
    final Children.Builder builder = Children.create();

    for (char letter = 'a'; letter <= 'z'; letter++) {
      builder
          .child(
              SingleComponentSection.create(c)
                  .key("header" + letter)
                  .component(Text.create(c).text("Header " + letter).textSizeDip(20).build())
                  .build())
          .child(
              DataDiffSection.<String>create(c)
                  .data(getStoriesContent())
                  .key("dds" + letter)
                  .renderEventHandler(StoryCardsWithHeaderSection.renderStory(c))
                  .build());
    }

    return builder.build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo renderStory(SectionContext c, @FromEvent String model, @FromEvent int index) {
    return ComponentRenderInfo.create()
        .component(
            StoryCardComponent.create(c)
                .content(model)
                .header(
                    StoryHeaderComponent.create(c)
                        .title("Story #" + index)
                        .subtitle("subtitle")
                        .build())
                .build())
        .build();
  }

  private static List getStoriesContent() {
    final List<String> contents = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      contents.add(
          "StoryCard #"
              + i
              + ": This is some test content. It should fill at least "
              + "one line. This is a story card. You can interact with the menu button "
              + "and save button.");
    }

    return contents;
  }
}
