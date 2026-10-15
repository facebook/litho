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

package com.facebook.samples.litho.kotlin.commonprops

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useCached
import com.facebook.litho.view.clip
import com.facebook.litho.view.clipToOutline
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp

class RoundedCornersComponent(
    private val cornerRadius: Dimen = 2.dp,
    private val style: Style = Style,
    private val child: () -> Component,
) : KComponent() {
  override fun ComponentScope.render(): Component? {
    val cornerRadiusFloat = cornerRadius.toPixels().toFloat()
    val outlineProvider = useCached(cornerRadiusFloat) { RoundedOutlineProvider(cornerRadiusFloat) }
    return Column(style = style.clip(outlineProvider).clipToOutline(true)) { child(child()) }
  }
}
