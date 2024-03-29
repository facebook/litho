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

package com.facebook.samples.litho.java.errors;

import android.graphics.Typeface;
import android.util.Log;
import androidx.annotation.ColorInt;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.utils.StacktraceHelper;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

/**
 * Renders a throwable as a text with a title and provides a touch callback that logs the throwable
 * with WTF level.
 */
@LayoutSpec
public class DebugErrorComponentSpec {

  private static final String TAG = "DebugErrorComponentSpec";

  private static final @ColorInt int DARK_RED_FRAME = 0xffcd4928;
  private static final @ColorInt int LIGHT_RED_BACKGROUND = 0xfffcece9;
  private static final @ColorInt int LIGHT_GRAY_TEXT = 0xff606770;

  @OnCreateLayout
  public static Component onCreateLayout(
      ComponentContext c, @Prop String message, @Prop Throwable throwable) {
    Log.e(TAG, message, throwable);

    return Column.create(c)
        .backgroundColor(DARK_RED_FRAME)
        .paddingDip(YogaEdge.ALL, 1f)
        .child(
            Text.create(c)
                .backgroundColor(LIGHT_RED_BACKGROUND)
                .paddingDip(YogaEdge.ALL, 4f)
                .textSizeDip(16f)
                .text(message))
        .child(
            Text.create(c)
                .backgroundColor(LIGHT_RED_BACKGROUND)
                .paddingDip(YogaEdge.ALL, 4f)
                .textSizeDip(12f)
                .textColor(LIGHT_GRAY_TEXT)
                .typeface(Typeface.MONOSPACE)
                .text(StacktraceHelper.formatStacktrace(throwable)))
        .clickHandler(DebugErrorComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @Prop String message, @Prop Throwable throwable) {
    Log.wtf(TAG, message, throwable);
  }
}
