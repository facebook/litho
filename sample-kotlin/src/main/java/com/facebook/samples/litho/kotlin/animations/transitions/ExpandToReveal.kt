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

package com.facebook.samples.litho.kotlin.animations.transitions

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.transition
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.view.alpha
import com.facebook.litho.view.clipChildren
import com.facebook.rendercore.dp

/**
 * Reveals [content] by growing from zero height to its measured height while fading in. The size
 * transition uses reflow, so the components below follow the resize every frame.
 */
@OptIn(ExperimentalLithoApi::class)
class ExpandToReveal(
    private val isExpanded: Boolean,
    private val content: Component,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val revealStyle = if (isExpanded) Style.alpha(1f) else Style.height(0.dp).alpha(0f)
    return Column(
        style =
            revealStyle.clipChildren(true).transition {
              size(spring(), reflow = true)
              alpha = spring()
            },
    ) {
      child(content)
    }
  }
}
