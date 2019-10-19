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

package com.facebook.samples.litho.errors;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Card;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class ListRowComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop ListRow row) {
    return Column.create(c)
        .paddingDip(YogaEdge.VERTICAL, 8)
        .paddingDip(YogaEdge.HORIZONTAL, 32)
        .child(
            Card.create(c)
                .content(
                    Column.create(c)
                        .marginDip(YogaEdge.ALL, 32)
                        .child(TitleComponent.create(c).title(row.title))
                        .child(PossiblyCrashingSubTitleComponent.create(c).subtitle(row.subtitle))
                        .build())
                .build())
        .build();
  }
}
