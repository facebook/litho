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
import android.widget.TextView;
import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.widget.treeprops.SimpleTreeProp;

@MountSpec(isPureRender = true)
class MountSpecWithTreePropSpec {

  @OnMeasure
  static void onMeasure(
      final ComponentContext c,
      final ComponentLayout layout,
      final int widthSpec,
      final int heightSpec,
      final Size size,
      final @Prop int viewWidth,
      final @Prop int viewHeight) {
    size.width = viewWidth;
    size.height = viewHeight;
  }

  @UiThread
  @OnCreateMountContent
  static TextView onCreateMountContent(Context c) {
    return new TextView(c);
  }

  @UiThread
  @OnMount
  static void onMount(
      final ComponentContext c,
      final TextView view,
      final @State int count,
      @TreeProp SimpleTreeProp treeProp) {
    view.setText(treeProp.name);
  }
}
