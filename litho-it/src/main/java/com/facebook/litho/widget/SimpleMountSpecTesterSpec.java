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

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;

@MountSpec
class SimpleMountSpecTesterSpec {

  @OnMeasure
  static void onMeasure(
      ComponentContext context, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {

    size.width = SizeSpec.getSize(widthSpec);
    size.height = SizeSpec.getSize(heightSpec);
  }

  @UiThread
  @OnCreateMountContent
  static Drawable onCreateMountContent(Context c) {
    return new ColorDrawable();
  }
}
