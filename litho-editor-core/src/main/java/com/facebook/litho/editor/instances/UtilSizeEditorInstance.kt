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

package com.facebook.litho.editor.instances

import android.util.Size
import com.facebook.litho.editor.Editor
import com.facebook.litho.editor.Reflection
import com.facebook.litho.editor.model.EditorString
import com.facebook.litho.editor.model.EditorValue
import java.lang.reflect.Field
import java.util.IllegalFormatException
import java.util.Locale

/**
 * An editor for android.util.Size, which is represented by the text "width=WIDTH_NUMBER
 * height=HEIGHT_NUMBER", where both numbers are integers.
 */
class UtilSizeEditorInstance : Editor {
  override fun read(f: Field, node: Any?): EditorValue {
    val size = Reflection.getValueUNSAFE<Size>(f, node) ?: return EditorValue.string("null")
    return try {
      EditorValue.string(
          String.format(
              Locale.ENGLISH,
              "%s=%d %s=%d",
              WIDTH_FIELD_STR,
              size.width,
              HEIGHT_FIELD_STR,
              size.height))
    } catch (e: IllegalFormatException) {
      EditorValue.string("null")
    }
  }

  // Assuming 'values' is an EditorString formatted as "width=WIDTH_NUMBER height=HEIGHT_NUMBER",
  // allowing extra whitespaces between 'height', 'width', '=', and both numbers, and assuming both
  // numbers are integers, we write them as the corresponding Size object.
  // In all other cases, nothing changes.
  override fun write(f: Field, node: Any?, values: EditorValue): Boolean {
    values.`when`<Void>(
        object : EditorValue.DefaultEditorVisitor() {
          override fun isString(editor: EditorString): Void? {
            val tokens: Array<String> = editor.value.split("=|\\s").toTypedArray()
            val components = ArrayList<String>()
            for (token in tokens) {
              if (token.isNotEmpty()) {
                components.add(token)
              }
            }
            if (components.size != 4 ||
                (components[0] != WIDTH_FIELD_STR) ||
                (components[2] != HEIGHT_FIELD_STR)) {
              return null
            }
            try {
              val sizeWidth = components[1].toInt()
              val sizeHeight = components[3].toInt()
              Reflection.setValueUNSAFE(f, node, Size(sizeWidth, sizeHeight))
            } catch (e: NumberFormatException) {
              // No-Op if value could not be parsed into a number.
            }
            return null
          }
        })
    return true
  }

  companion object {
    private const val WIDTH_FIELD_STR = "width"
    private const val HEIGHT_FIELD_STR = "height"
  }
}
