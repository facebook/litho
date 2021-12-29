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

package com.facebook.samples.litho.documentation.props;

import android.content.res.Resources;
import androidx.core.content.ContextCompat;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.samples.litho.R;

@LayoutSpec
public class PropResourceTypeParentComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop boolean useResourceType) {
    if (!useResourceType) {
      // start_prop_without_resource_type_usage
      final Resources res = c.getResources();

      return PropWithoutResourceTypeComponent.create(c)
          .name(res.getString(R.string.name_string))
          .size(res.getDimensionPixelSize(R.dimen.primary_text_size))
          .color(ContextCompat.getColor(c.getAndroidContext(), R.color.primaryColor))
          .build();
      // end_prop_without_resource_type_usage
    } else {
      // start_prop_with_resource_type_usage

      return PropWithResourceTypeComponent.create(c)
          .nameRes(R.string.name_string)
          .sizePx(10)
          .sizeDip(10)
          .colorAttr(android.R.attr.textColorTertiary)
          .build();
      // end_prop_with_resource_type_usage
    }
  }
}
