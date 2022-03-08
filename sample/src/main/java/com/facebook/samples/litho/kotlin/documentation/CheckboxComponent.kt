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

package com.facebook.samples.litho.kotlin.documentation

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.drawableRes
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.useState
import com.facebook.litho.view.onClick

// start_example
class CheckboxComponent : KComponent() {
  override fun ComponentScope.render(): Component? {
    val isChecked = useState { false }

    return Column(style = Style.onClick { isChecked.update { currValue -> !currValue } }) {
      child(
          Image(
              drawable =
                  drawableRes(
                      if (isChecked.value) {
                        android.R.drawable.checkbox_on_background
                      } else {
                        android.R.drawable.checkbox_off_background
                      })))
    }
  }
}
// end_example
