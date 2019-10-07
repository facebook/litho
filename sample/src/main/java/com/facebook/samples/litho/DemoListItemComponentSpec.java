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

package com.facebook.samples.litho;

import static com.facebook.yoga.YogaEdge.ALL;

import android.content.Intent;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class DemoListItemComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop final DemoListActivity.DemoListDataModel model) {
    return Column.create(c)
        .paddingDip(ALL, 16)
        .child(Text.create(c).text(model.name).textSizeSp(18).build())
        .clickHandler(DemoListItemComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @FromEvent View view,
      @Prop final DemoListActivity.DemoListDataModel model,
      @Prop final int[] currentIndices) {
    final Intent intent =
        new Intent(
            c.getAndroidContext(), model.datamodels == null ? model.klass : DemoListActivity.class);
    intent.putExtra(DemoListActivity.INDICES, currentIndices);
    c.getAndroidContext().startActivity(intent);
  }
}
