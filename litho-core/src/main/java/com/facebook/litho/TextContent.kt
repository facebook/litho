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

package com.facebook.litho

import android.graphics.Typeface
import com.facebook.proguard.annotations.DoNotStrip

/**
 * An UI Element that contains textual content. This element can have more than one textual content.
 * For example, one [Mountable] wrapping a complex custom view can have multiple text views, and
 * therefore, multiple [TextContent.Item]
 *
 * It is responsibility of who implements the [Mountable] to implement this interface and provide
 * the correct properties.
 */
@DoNotStrip
interface TextContent {

  /** @return the list of text items that are rendered by this UI element. */
  @get:DoNotStrip val items: List<Item>

  /**
   * This is a helper method to retrieve the [CharSequence] that make up the textual content in this
   * given [TextContent]
   */
  val textList: List<CharSequence>

  interface Item {

    val text: CharSequence

    val textSize: Float

    val fontLineHeight: Float

    val typeface: Typeface

    val color: Int

    val linesCount: Int
  }
}
