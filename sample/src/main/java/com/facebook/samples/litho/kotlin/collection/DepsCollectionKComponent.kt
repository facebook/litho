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

package com.facebook.samples.litho.kotlin.collection

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sp
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.widget.collection.LazyList

class DepsCollectionKComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val color = useState { Color.RED }
    val size = useState { 14 }
    val alpha = useState { 1f }

    return Column(style = Style.padding(16.dp)) {
      child(
          Row {
            child(
                Button("Toggle Color") {
                  color.update { prevColor ->
                    if (prevColor == Color.RED) Color.BLUE else Color.RED
                  }
                })
            child(
                Button("Toggle Size") {
                  size.update { prevSize -> if (prevSize == 14) 28 else 14 }
                })
            child(
                Button("Toggle Alpha") {
                  alpha.update { prevAlpha -> if (prevAlpha == 0.5f) 1f else 0.5f }
                })
          })
      child(
          LazyList {
            child(
                Text(
                    "deps = null (all props)",
                    textColor = color.value,
                    textSize = size.value.sp,
                    style = Style.alpha(alpha.value)))
            child(deps = arrayOf(color.value)) {
              Text(
                  "deps = arrayOf(color.value)",
                  textColor = color.value,
                  textSize = size.value.sp,
                  style = Style.alpha(alpha.value))
            }
            child(deps = arrayOf(size.value)) {
              Text(
                  "deps = arrayOf(size.value)",
                  textColor = color.value,
                  textSize = size.value.sp,
                  style = Style.alpha(alpha.value))
            }
            child(deps = arrayOf()) {
              Text(
                  "deps = arrayOf()",
                  textColor = color.value,
                  textSize = size.value.sp,
                  style = Style.alpha(alpha.value))
            }
          })
    }
  }
}
