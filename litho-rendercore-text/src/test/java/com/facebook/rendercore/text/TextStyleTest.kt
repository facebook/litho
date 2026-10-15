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

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import androidx.core.text.TextDirectionHeuristicCompat
import java.lang.reflect.Modifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextStyleTest {

  @Test
  fun `a copy has the same values`() {
    val style = TextStyle().apply { textSize = 40 }

    assertThat(style.makeCopy().hasSameValuesAs(style)).isTrue()
  }

  @Test
  fun `changing any field gives different values`() {
    val unchanged =
        TextStyle::class
            .java
            .declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .mapNotNull { field ->
              val copy = TextStyle().makeCopy()
              field.isAccessible = true
              field.set(copy, differentValue(field.type, field.get(copy)))
              if (copy.hasSameValuesAs(TextStyle())) field.name else null
            }

    assertThat(unchanged).isEmpty()
  }

  @Test
  fun `text directions that print the same but differ are different values`() {
    val ltr = TextStyle().apply { textDirection = SameNameDirection(isRtl = false) }
    val rtl = ltr.makeCopy().apply { textDirection = SameNameDirection(isRtl = true) }

    assertThat(ltr.textDirection.toString()).isEqualTo(rtl.textDirection.toString())
    assertThat(rtl.hasSameValuesAs(ltr)).isFalse()
  }

  private class SameNameDirection(private val isRtl: Boolean) : TextDirectionHeuristicCompat {
    override fun isRtl(array: CharArray, start: Int, count: Int): Boolean = isRtl

    override fun isRtl(cs: CharSequence, start: Int, count: Int): Boolean = isRtl

    override fun toString(): String = "direction"
  }

  private fun differentValue(type: Class<*>, current: Any?): Any? =
      when (type) {
        Int::class.javaPrimitiveType -> (current as Int) + 1
        // Halved rather than incremented: some defaults are Float.MAX_VALUE, where +1 changes
        // nothing.
        Float::class.javaPrimitiveType -> (current as Float).let { if (it == 0f) 1f else it / 2f }
        Boolean::class.javaPrimitiveType -> !(current as Boolean)
        java.lang.Boolean::class.java -> if (current == true) false else true
        CharSequence::class.java -> "${current ?: ""}x"
        String::class.java -> "${current ?: ""}x"
        TextUtils.TruncateAt::class.java ->
            if (current == TextUtils.TruncateAt.END) TextUtils.TruncateAt.START
            else TextUtils.TruncateAt.END
        Typeface::class.java ->
            if (current == Typeface.SERIF) Typeface.MONOSPACE else Typeface.SERIF
        TextDirectionHeuristicCompat::class.java -> SameNameDirection(isRtl = true)
        ColorStateList::class.java -> ColorStateList.valueOf(Color.RED)
        TextStyle.RoundedBackgroundProps::class.java ->
            TextStyle.RoundedBackgroundProps(RectF(1f, 1f, 1f, 1f), 1f, Color.RED)
        else ->
            if (type.isEnum) {
              type.enumConstants.first { it != current }
            } else {
              error("Add a different value for ${type.name}")
            }
      }
}
