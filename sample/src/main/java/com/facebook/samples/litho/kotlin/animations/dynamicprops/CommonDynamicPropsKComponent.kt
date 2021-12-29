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

package com.facebook.samples.litho.kotlin.animations.dynamicprops

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animated.alpha
import com.facebook.litho.animated.scaleX
import com.facebook.litho.animated.scaleY
import com.facebook.litho.animated.useBinding
import com.facebook.litho.colorRes
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.view.backgroundColor
import com.facebook.samples.litho.R
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

// start_example
class CommonDynamicPropsKComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val scale = useBinding(1f)
    val alpha = useBinding(1f)

    val square =
        Column(
            style =
                Style.width(100.dp)
                    .height(100.dp)
                    .backgroundColor(colorRes(R.color.primaryColor))
                    .alignSelf(YogaAlign.CENTER)
                    .scaleX(scale)
                    .scaleY(scale)
                    .alpha(alpha))

    return Column(justifyContent = YogaJustify.SPACE_BETWEEN, style = Style.padding(all = 20.dp)) {
      child(SeekBar(onProgressChanged = { alpha.set(it) }))
      child(square)
      child(SeekBar(onProgressChanged = { scale.set(it) }))
    }
  }
}
// end_example
