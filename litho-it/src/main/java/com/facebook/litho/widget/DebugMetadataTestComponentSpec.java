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
import com.facebook.litho.DebugMetadata;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TreeProp;

@LayoutSpec
class DebugMetadataTestComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Nullable @Prop(optional = true) Component child) {
    return Column.create(c).child(child).build();
  }

  @OnCreateTreeProp
  static DebugMetadata createDebugMetadata(
      ComponentContext c,
      @Prop String metadataKey,
      @Prop String metadataValue,
      @TreeProp DebugMetadata existingMetadata) {
    return DebugMetadata.createWithMetadata(existingMetadata, metadataKey, metadataValue);
  }
}
