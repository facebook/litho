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

package com.facebook.samples.litho.animations.transitions;

import android.graphics.Color;
import androidx.annotation.Dimension;
import com.facebook.litho.Animations;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.SingleComponentSection;
import java.util.ArrayList;
import java.util.List;

@GroupSectionSpec
class MenuItemsSectionSpec {

  private static final int NUM_TILES = 10;

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c, @Prop DynamicValue<Float> expandedAmount) {
    final List<Section> sections = new ArrayList<>(NUM_TILES);
    final @Dimension int tileWidthWithSpacing = c.getResourceResolver().dipsToPixels(55);
    for (int i = 0; i < NUM_TILES; i++) {
      final DynamicValue<Float> translationX =
          Animations.bind(expandedAmount).outputRange(-tileWidthWithSpacing * (i + 1), 0).create();
      final DynamicValue<Float> scale =
          Animations.bind(expandedAmount).outputRange(0.8f, 1).create();

      sections.add(
          SingleComponentSection.create(c)
              .component(
                  TileComponent.create(c)
                      .alpha(expandedAmount)
                      .translationX(translationX)
                      .scaleX(scale)
                      .scaleY(scale)
                      .bgColor(Color.RED)
                      .text("" + i))
              .build());
    }
    return Children.create().child(sections).build();
  }
}
