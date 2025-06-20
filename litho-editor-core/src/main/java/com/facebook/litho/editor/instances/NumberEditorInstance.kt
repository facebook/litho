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

import com.facebook.litho.editor.Editor
import com.facebook.litho.editor.Reflection
import com.facebook.litho.editor.model.EditorNumber
import com.facebook.litho.editor.model.EditorValue
import java.lang.reflect.Field

class NumberEditorInstance<T : Number?>(private val clazz: Class<T>) : Editor {
  override fun read(f: Field, node: Any?): EditorValue {
    // If you use Number here it causes an exception when attempting to do implicit conversion
    val n = Reflection.getValueUNSAFE<Any>(f, node)
    return if (n == null) EditorValue.string("null")
    else EditorValue.number((n as Number).toFloat())
  }

  override fun write(f: Field, node: Any?, values: EditorValue): Boolean {
    values.`when`(
        object : EditorValue.DefaultEditorVisitor() {
          override fun isNumber(number: EditorNumber): Void? {
            var value: Number = number.value
            when (clazz) {
              Int::class.javaPrimitiveType,
              Int::class.java -> {
                value = value.toInt()
              }
              Float::class.javaPrimitiveType,
              Float::class.java -> {
                value = value.toFloat()
              }
              Short::class.javaPrimitiveType,
              Short::class.java -> {
                value = value.toShort()
              }
              Byte::class.javaPrimitiveType,
              Byte::class.java -> {
                value = value.toByte()
              }
              Double::class.javaPrimitiveType,
              Double::class.java -> {
                value = value.toDouble()
              }
              Long::class.javaPrimitiveType,
              Long::class.java -> {
                value = value.toLong()
              }
            } // else trust the value the user has input is assignable

            // to the one expected by the field
            Reflection.setValueUNSAFE(f, node, value)
            return null
          }
        })
    return true
  }
}
