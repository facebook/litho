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

package com.facebook.samples.litho.java.stats;

import android.graphics.Color;
import android.graphics.Typeface;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.stats.LithoStats;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.VerticalScroll;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class StatsSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return VerticalScroll.create(c)
        .backgroundColor(Color.WHITE)
        .paddingDip(YogaEdge.ALL, 5f)
        .childComponent(
            Column.create(c)
                .child(
                    Text.create(c)
                        .textSizeSp(18)
                        .text("COMPONENTS")
                        .paddingDip(YogaEdge.BOTTOM, 8f))
                .child(
                    getStatsText(
                        c,
                        "Total applied state updates:                   "
                            + LithoStats.getComponentAppliedStateUpdateCount()))
                .child(
                    getStatsText(
                        c,
                        "Total triggered *sync* state updates:          "
                            + LithoStats.getComponentTriggeredSyncStateUpdateCount()))
                .child(
                    getStatsText(
                        c,
                        "Total triggered *async* state updates:         "
                            + LithoStats.getComponentTriggeredAsyncStateUpdateCount()))
                .child(
                    getStatsText(
                        c,
                        "Total count of layout calculations:            "
                            + LithoStats.getComponentCalculateLayoutCount()))
                .child(
                    getStatsText(
                        c,
                        "Total count of layout calculations on UI:      "
                            + LithoStats.getComponentCalculateLayoutOnUICount()))
                .child(
                    getStatsText(
                        c,
                        "Total amount of component mounts:              "
                            + LithoStats.getComponentMountCount()))
                .child(
                    Text.create(c)
                        .textSizeSp(18)
                        .text("SECTIONS")
                        .paddingDip(YogaEdge.TOP, 8f)
                        .paddingDip(YogaEdge.BOTTOM, 8f))
                .child(
                    getStatsText(
                        c,
                        "Total applied state updates:                   "
                            + LithoStats.getSectionAppliedStateUpdateCount()))
                .child(
                    getStatsText(
                        c,
                        "Total triggered *sync* state updates:          "
                            + LithoStats.getSectionTriggeredSyncStateUpdateCount()))
                .child(
                    getStatsText(
                        c,
                        "Total triggered *async* state updates:         "
                            + LithoStats.getSectionTriggeredAsyncStateUpdateCount()))
                .child(
                    getStatsText(
                        c,
                        "Total count of changeset calculations:         "
                            + LithoStats.getSectionCalculateNewChangesetCount()))
                .child(
                    getStatsText(
                        c,
                        "Total count of changeset calculations on UI:   "
                            + LithoStats.getSectionCalculateNewChangesetOnUICount())))
        .build();
  }

  private static Component getStatsText(ComponentContext c, String text) {
    return Text.create(c)
        .textSizeSp(12)
        .paddingDip(YogaEdge.BOTTOM, 4)
        .textColor(Color.DKGRAY)
        .textStyle(Typeface.ITALIC)
        .typeface(Typeface.MONOSPACE)
        .text(text)
        .build();
  }
}
