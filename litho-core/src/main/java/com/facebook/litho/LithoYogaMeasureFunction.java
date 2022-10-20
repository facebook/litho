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

package com.facebook.litho;

import android.annotation.SuppressLint;
import com.facebook.rendercore.LayoutContext;
import com.facebook.rendercore.MeasureResult;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;

public class LithoYogaMeasureFunction implements YogaMeasureFunction {

  @Override
  @SuppressLint("WrongCall")
  @SuppressWarnings("unchecked")
  public long measure(
      YogaNode cssNode,
      float width,
      YogaMeasureMode widthMode,
      float height,
      YogaMeasureMode heightMode) {
    final LayoutContext context = LithoLayoutResult.getLayoutContextFromYogaNode(cssNode);
    final LithoLayoutResult result = LithoLayoutResult.getLayoutResultFromYogaNode(cssNode);
    final int widthSpec = SizeSpec.makeSizeSpecFromCssSpec(width, widthMode);
    final int heightSpec = SizeSpec.makeSizeSpecFromCssSpec(height, heightMode);
    final MeasureResult size = result.measure(context, widthSpec, heightSpec);
    return YogaMeasureOutput.make(size.width, size.height);
  }
}
