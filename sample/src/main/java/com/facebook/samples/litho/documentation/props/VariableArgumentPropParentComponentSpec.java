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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.samples.litho.R;
import java.util.Arrays;

@LayoutSpec
public class VariableArgumentPropParentComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop boolean useVarArgWithResType) {
    if (useVarArgWithResType) {
      // start_var_arg_res_type_usage
      return VariableArgumentWithResourceType.create(c)
          .name("One")
          .nameRes(R.string.app_name)
          .nameRes(R.string.name_string)
          .build();
      // end_var_arg_res_type_usage
    } else {
      // start_var_arg_usage
      return VariableArgumentPropComponent.create(c)
          .name("One")
          .name("Two")
          .name("Three")
          .names(Arrays.asList("Four", "Five", "Six"))
          .build();
      // end_var_arg_usage
    }
  }
}
