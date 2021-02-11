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

import androidx.annotation.Nullable;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Output;
import com.facebook.litho.Row;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;

@LayoutSpec
public class LayoutSpecVisibilityEventTesterSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .wrapInView()
        .visibleHandler(LayoutSpecVisibilityEventTester.onViewVisible(c))
        .child(
            Row.create(c)
                .visibleHandler(LayoutSpecVisibilityEventTester.onNotAViewVisible(c))
                .child(
                    Text.create(c)
                        .text("test1")
                        .visibleHandler(LayoutSpecVisibilityEventTester.onTextVisible(c))
                        .textSizeDip(30)))
        .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onTextVisible(
      final ComponentContext c,
      final @Prop Output<Object> textOutput,
      final @FromEvent @Nullable Object content) {
    textOutput.set(content);
  }

  @OnEvent(VisibleEvent.class)
  static void onViewVisible(
      final ComponentContext c,
      final @Prop Output<Object> viewOutput,
      final @FromEvent @Nullable Object content) {
    viewOutput.set(content);
  }

  @OnEvent(VisibleEvent.class)
  static void onNotAViewVisible(
      final ComponentContext c,
      final @Prop Output<Object> nullOutput,
      final @FromEvent @Nullable Object content) {
    nullOutput.set(content);
  }
}
