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
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.Output;
import com.facebook.litho.Row;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.Wrapper;
import com.facebook.litho.annotations.FromPreviousCreateLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.OnShouldCreateLayoutWithNewSizeSpec;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.events.EventWithoutAnnotation;
import java.util.List;

@LayoutSpec(events = EventWithoutAnnotation.class)
public class LayoutSpecInterStagePropsTesterSpec {

  @OnCreateLayoutWithSizeSpec
  static Component onCreateLayout(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      @Prop List<LifecycleStep.StepInfo> steps,
      Output<Boolean> shouldLayoutWithSizeSpec) {
    shouldLayoutWithSizeSpec.set(true);
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC));
    return Column.create(c)
        .child(
            Wrapper.create(c)
                .delegate(
                    DelegatingLayout.create(c)
                        .delegate(
                            SolidColor.create(c)
                                .color(Color.BLACK)
                                .widthPx(SizeSpec.getSize(widthSpec)))
                        .build())
                .build())
        .child(Row.create(c).heightPx(SizeSpec.getSize(heightSpec)))
        .build();
  }

  @OnShouldCreateLayoutWithNewSizeSpec
  static boolean onShouldCreateLayoutWithNewSizeSpec(
      ComponentContext c,
      int newWidthSpec,
      int newHeightSpec,
      @Prop List<LifecycleStep.StepInfo> steps,
      @FromPreviousCreateLayout Boolean shouldLayoutWithSizeSpec) {
    if (shouldLayoutWithSizeSpec) {
      steps.add(
          new LifecycleStep.StepInfo(LifecycleStep.ON_SHOULD_CREATE_LAYOUT_WITH_NEW_SIZE_SPEC));
    }
    return shouldLayoutWithSizeSpec;
  }
}
