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

package com.facebook.samples.litho.kotlin.gettingstartedsolution

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp

/**
 * Components which consists in a vertical list with all characters from the given sentence.
 *
 * @property text the sentence to be displayed vertically
 */
class VerticalSpeller(private val text: String) : KComponent() {
  override fun ComponentScope.render(): Component {
    return LazyList(style = Style.flex(grow = 1f)) {
      child(Text(text = "Spelling of $text"))
      // Using index as id is recommended here as we may have the same character twice in the text
      text.forEachIndexed { index, character ->
        child(
            id = index,
            component = Text(style = Style.margin(horizontal = 16.dp), text = character.toString()))
      }
    }
  }
}
