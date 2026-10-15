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

import com.facebook.litho.DynamicValue
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.useCached
import com.facebook.rendercore.RenderCoreConfig
import com.facebook.rendercore.text.ClickableSpanAccessibilityProvider
import com.facebook.rendercore.text.ClickableSpanListener
import com.facebook.rendercore.text.RCTextView
import com.facebook.rendercore.text.RichTextPrimitive
import com.facebook.rendercore.text.TextStyle
import com.facebook.rendercore.text.TouchableSpanListener

/**
 * @param useLegacyTextCustomEllipsisOffsetFix With performant truncation, cut a custom ellipsis at
 *   the offset `Paint.getOffsetForAdvance` returns instead of one character before it, like legacy
 *   Text with `enableFixForTextEllipsisOffset`, stepping back while it doesn't fit. Off by default
 *   so existing RichText users keep their current truncation behavior.
 * @param useResolvedLayoutDirection Resolve text direction and LAYOUT_START/LAYOUT_END from this
 *   component's Litho (Yoga) layout direction instead of the locale, matching Litho's legacy Text.
 *   Off by default so existing RichText users keep their locale-based behaviour; see
 *   [com.facebook.rendercore.text.RichTextPrimitive] for why.
 */
class RichText(
    private val text: CharSequence,
    private val textStyle: TextStyle? = null,
    private val style: Style = Style,
    private val touchableSpanListener: TouchableSpanListener? = null,
    private val clickableSpanListener: ClickableSpanListener? = null,
    private val usePerformantTruncation: Boolean = RenderCoreConfig.usePerformantTruncation,
    private val useTruncationCaching: Boolean = RenderCoreConfig.useTruncationCaching,
    private val dynamicTextColor: DynamicValue<Int?>? = null,
    private val clickableSpanAccessibilityProvider: ClickableSpanAccessibilityProvider? = null,
    private val useResolvedLayoutDirection: Boolean = false,
    private val useLegacyTextCustomEllipsisOffsetFix: Boolean = false,
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val defaultTextStyle = useCached {
      lazy(LazyThreadSafetyMode.NONE) { TextStyle.createDefaultConfiguredTextStyle(androidContext) }
    }
    return LithoPrimitive(
        RichTextPrimitive(
            id = createPrimitiveId(),
            text = text,
            style = textStyle ?: defaultTextStyle.value,
            touchableSpanListener = touchableSpanListener,
            clickableSpanListener = clickableSpanListener,
            clickableSpanAccessibilityProvider = clickableSpanAccessibilityProvider,
            usePerformantTruncation = usePerformantTruncation,
            useLegacyTextCustomEllipsisOffsetFix = useLegacyTextCustomEllipsisOffsetFix,
            useTruncationCaching = useTruncationCaching,
            hasIntrinsicSize = ComponentsConfiguration.useAutoMinSize,
            // Always registered, even without a value: a render unit's fixed binders must keep the
            // same count across updates, so a Text that gains or loses a dynamic color can update
            // in place.
            extraMountConfiguration = {
              bindDynamic(dynamicTextColor, RCTextView::setDynamicTextColor)
            },
            useResolvedLayoutDirection = useResolvedLayoutDirection,
        ),
        style = style,
    )
  }
}
