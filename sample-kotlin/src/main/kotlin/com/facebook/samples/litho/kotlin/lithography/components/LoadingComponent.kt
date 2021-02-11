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

package com.facebook.samples.litho.kotlin.lithography.components

import android.graphics.Color
import com.facebook.litho.DslScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.dp
import com.facebook.litho.flexbox.height
import com.facebook.litho.flexbox.width
import com.facebook.litho.widget.Progress
import com.facebook.yoga.YogaJustify.CENTER

class LoadingComponent : KComponent() {
  override fun DslScope.render() =
      Row(
          justifyContent = CENTER,
          children =
              listOf(
                  Progress(color = Color.DKGRAY, style = Style.width(50.dp).height(50.dp)),
              ))
}
