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

import android.content.res.Resources
import androidx.core.content.ContextCompat
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.rendercore.colorRes
import com.facebook.rendercore.dimenRes
import com.facebook.rendercore.stringRes
import com.facebook.samples.litho.R

// start_example
class PropResourceTypeParentKComponent(
    private val useResourceType: Boolean,
) : KComponent() {

  override fun ComponentScope.render(): Component {

    if (!useResourceType) {
      // start_prop_without_resource_type_usage
      val res: Resources = context.resources
      return PropWithoutResourceTypeKComponent(
          name = res.getString(R.string.name_string),
          size = dimenRes(R.dimen.primary_text_size),
          color = ContextCompat.getColor(context.getAndroidContext(), R.color.primaryColor))
      // end_prop_without_resource_type_usage
    } else {
      // start_prop_with_resource_type_usage
      return PropWithResourceTypeKComponent(
          name = stringRes(R.string.name_string),
          size = dimenRes(R.dimen.primary_text_size),
          color = colorRes(R.color.primaryColor))
      // end_prop_with_resource_type_usage
    }
  }
}
// end_example
