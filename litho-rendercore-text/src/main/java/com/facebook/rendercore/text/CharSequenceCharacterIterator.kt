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

package com.facebook.rendercore.text

import java.text.CharacterIterator

/**
 * A [CharacterIterator] that reads directly from a [CharSequence] over the half-open range
 * [[start], [end]), avoiding the [CharSequence.toString] copy that
 * [java.text.BreakIterator.setText] would otherwise force. Ported from AndroidX's internal
 * `CharSequenceCharacterIterator`.
 */
internal class CharSequenceCharacterIterator(
    private val charSequence: CharSequence,
    private val start: Int,
    private val end: Int,
) : CharacterIterator {

  private var index = start

  override fun first(): Char {
    index = start
    return current()
  }

  override fun last(): Char {
    if (start == end) {
      index = end
      return CharacterIterator.DONE
    }
    index = end - 1
    return charSequence[index]
  }

  override fun current(): Char = if (index == end) CharacterIterator.DONE else charSequence[index]

  override fun next(): Char {
    index++
    if (index >= end) {
      index = end
      return CharacterIterator.DONE
    }
    return current()
  }

  override fun previous(): Char {
    if (index <= start) {
      return CharacterIterator.DONE
    }
    index--
    return current()
  }

  override fun setIndex(position: Int): Char {
    require(position in start..end) { "invalid position: $position" }
    index = position
    return current()
  }

  override fun getBeginIndex(): Int = start

  override fun getEndIndex(): Int = end

  override fun getIndex(): Int = index

  override fun clone(): Any {
    val copy = CharSequenceCharacterIterator(charSequence, start, end)
    copy.index = index
    return copy
  }
}
