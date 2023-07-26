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

package com.facebook.samples.litho.documentation.props

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.rendercore.stringRes
import com.facebook.samples.litho.R

class VariableArgumentPropParentKComponent(private val useVarArgWithResType: Boolean) :
    KComponent() {

  override fun ComponentScope.render(): Component {
    if (useVarArgWithResType) {
      // start_var_arg_res_type_usage
      return VariableArgumentPropKComponent(
          "One", stringRes(R.string.app_name), stringRes(R.string.name_string))
      // end_var_arg_res_type_usage
    } else {
      // start_var_arg_usage
      return VariableArgumentPropKComponent("One", "Two", "Three", "Four")
      // end_var_arg_usage
    }
  }
}
