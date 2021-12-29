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

package com.facebook.samples.litho.documentation.treeprops;

import android.net.Uri;
import androidx.annotation.Nullable;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.widget.Text;

// start_example
@LayoutSpec
class ChildComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop Uri imageUri, @Nullable @TreeProp Prefetcher prefetcher) {
    if (prefetcher != null) {
      prefetcher.prefetch(imageUri);
    }
    // ...
    return Column.create(c)
        .child(Text.create(c).text("Prefetch image when this component is created"))
        .build();
  }
}
// end_example
