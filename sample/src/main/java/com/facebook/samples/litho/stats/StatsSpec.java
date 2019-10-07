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

package com.facebook.samples.litho.stats;

import android.graphics.Color;
import android.graphics.Typeface;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.stats.LithoStats;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class StatsSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .paddingDip(YogaEdge.ALL, 5f)
        .child(Text.create(c).textSizeSp(20).text("LITHO STATS").marginDip(YogaEdge.BOTTOM, 10f))
        .child(
            Text.create(c)
                .textSizeSp(14)
                .textColor(Color.DKGRAY)
                .textStyle(Typeface.ITALIC)
                .typeface(Typeface.MONOSPACE)
                .text(
                    "Total applied state updates:           "
                        + LithoStats.getAppliedStateUpdates()))
        .child(
            Text.create(c)
                .textSizeSp(14)
                .textColor(Color.DKGRAY)
                .textStyle(Typeface.ITALIC)
                .typeface(Typeface.MONOSPACE)
                .text("Total triggered *sync* state updates:  " + LithoStats.getStateUpdatesSync()))
        .child(
            Text.create(c)
                .textSizeSp(14)
                .textColor(Color.DKGRAY)
                .textStyle(Typeface.ITALIC)
                .typeface(Typeface.MONOSPACE)
                .text(
                    "Total triggered *async* state updates: " + LithoStats.getStateUpdatesAsync()))
        .child(
            Text.create(c)
                .textSizeSp(14)
                .textColor(Color.DKGRAY)
                .textStyle(Typeface.ITALIC)
                .typeface(Typeface.MONOSPACE)
                .text("Total triggered *lazy* state updates:  " + LithoStats.getStateUpdatesLazy()))
        .build();
  }
}
