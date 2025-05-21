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

package com.facebook.litho.widget

import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.useCached
import com.facebook.rendercore.RenderCoreConfig
import com.facebook.rendercore.text.RichTextPrimitive
import com.facebook.rendercore.text.TextStyle

class RichText(
    val text: CharSequence,
    val textStyle: TextStyle? = null,
    private val style: Style? = null,
    private val usePerformantTruncation: Boolean = RenderCoreConfig.usePerformantTruncation,
    private val useTruncationCaching: Boolean = RenderCoreConfig.useTruncationCaching
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val resolvedTextStyle =
        useCached(textStyle) {
          textStyle ?: TextStyle.createDefaultConfiguredTextStyle(androidContext)
        }
    return LithoPrimitive(
        RichTextPrimitive(
            createPrimitiveId(),
            text,
            resolvedTextStyle,
            usePerformantTruncation,
            useTruncationCaching),
        style = style)
  }
}
