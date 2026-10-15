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

package com.facebook.samples.litho.kotlin.widgets

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp
import com.facebook.samples.litho.kotlin.widgets.sections.AvoidingTextClippingSection
import com.facebook.samples.litho.kotlin.widgets.sections.BasicTextStylingSection
import com.facebook.samples.litho.kotlin.widgets.sections.ComplexLayoutsSection
import com.facebook.samples.litho.kotlin.widgets.sections.LayoutPositioningSection
import com.facebook.samples.litho.kotlin.widgets.sections.TextAlignmentSection

/**
 * Sample component demonstrating text widget usage including:
 * - Text styling (color, size, weight, alignment)
 * - Proper positioning and layout
 * - Avoiding common issues like text clipping
 * - Complex layouts with flex and avatars
 */
class TextWidgetSampleComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    return LazyList(style = Style.padding(all = 12.dp)) {
      child(id = "basic_styling", component = BasicTextStylingSection())
      child(id = "text_alignment", component = TextAlignmentSection())
      child(id = "layout_positioning", component = LayoutPositioningSection())
      child(id = "avoiding_clipping", component = AvoidingTextClippingSection())
      child(id = "complex_layouts", component = ComplexLayoutsSection())
    }
  }
}
