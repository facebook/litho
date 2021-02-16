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

package com.facebook.litho.widget;

import android.graphics.Color;
import androidx.annotation.Nullable;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.widget.ItemCardComponentSpec.TreeProps;

@LayoutSpec
class CardActionsComponentSpec {

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Boolean> isEnabled) {
    isEnabled.set(true);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @State boolean isEnabled,
      @TreeProp ItemCardComponentSpec.TreeProps props) {
    return Column.create(c)
        .child(Text.create(c).text("controls"))
        .backgroundColor(Color.GRAY)
        .enabled(props == null || !props.areCardToolsDisabled)
        .child(
            Row.create(c)
                .enabled(isEnabled)
                .wrapInView()
                .visibleHandler(CardActionsComponent.onVisible(c))
                .child(SolidColor.create(c).color(Color.RED).widthDip(25).heightDip(25))
                .child(SolidColor.create(c).color(Color.GREEN).widthDip(25).heightDip(25))
                .child(SolidColor.create(c).color(Color.BLUE).widthDip(25).heightDip(25)))
        .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(
      ComponentContext c,
      @Nullable @FromEvent Object content,
      @Nullable @TreeProp TreeProps props) {
    if (props != null && props.onCardActionViewVisible != null) {
      props.onCardActionViewVisible.call(content);
    }
  }
}
