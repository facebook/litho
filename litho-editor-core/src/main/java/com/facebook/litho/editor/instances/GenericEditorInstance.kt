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
import com.facebook.litho.editor.EditorRegistry.read
import com.facebook.litho.editor.Reflection.geFieldUNSAFE
import com.facebook.litho.editor.Reflection.getValueUNSAFE
import com.facebook.litho.editor.Reflection.setValueUNSAFE
import com.facebook.litho.editor.model.EditorBool
import com.facebook.litho.editor.model.EditorNumber
import com.facebook.litho.editor.model.EditorString
import com.facebook.litho.editor.model.EditorValue
import com.facebook.litho.editor.model.EditorValue.Companion.shape
import com.facebook.litho.editor.model.EditorValue.Companion.string
import com.facebook.litho.editor.model.EditorValue.DefaultEditorPrimitiveVisitor
import java.lang.reflect.Field

class GenericEditorInstance : Editor {
  internal fun interface GenericEditorValueWriter {
    fun write(obj: Any?, field: Field, fieldValue: Any?, value: EditorValue?): Boolean
  }

  override fun read(f: Field, node: Any?): EditorValue {
    val parsedValue = getValueUNSAFE<Any?>(f, node) ?: return string("null")

    val fields = parsedValue.javaClass.declaredFields
    val map = HashMap<String, EditorValue>()

    for (field in fields) {
      val value = getValueUNSAFE<Any>(field, parsedValue)
      if (value != null) {
        val editorValue = read(value.javaClass, field, parsedValue)
        if (editorValue != null) {
          map[field.name] = editorValue
        } else {
          map[field.name] = string(value.toString())
        }
      } else {
        map[field.name] = string("null")
      }
    }

    return shape(map)
  }

  override fun write(f: Field, node: Any?, v: EditorValue): Boolean {
    val parsedValue = getValueUNSAFE<Any?>(f, node)
    v.whenPrimitive(
        object : DefaultEditorPrimitiveVisitor() {
          fun write(
              obj: Any?,
              path: Array<String>,
              editorValue: EditorValue?,
              writer: GenericEditorValueWriter
          ): Boolean {
            var obj = obj ?: return false
            var clazz: Class<*> = obj.javaClass

            for (i in path.indices) {
              val field = geFieldUNSAFE(clazz, path[i])
              if (field == null) {
                return false
              }
              val value = getValueUNSAFE<Any>(field, obj) ?: return false

              if (i == path.lastIndex) {
                writer.write(obj, field, value, editorValue)
                return true
              }

              clazz = value.javaClass
              obj = value
            }

            return false
          }

          override fun isBool(path: Array<String>, bool: EditorBool): Boolean {
            return this.write(
                parsedValue,
                path,
                bool,
                GenericEditorValueWriter {
                    obj: Any?,
                    field: Field,
                    fieldValue: Any?,
                    value: EditorValue? ->
                  if (fieldValue is Boolean) {
                    setValueUNSAFE(field, obj, value?.getValue())
                    true
                  } else {
                    false
                  }
                },
            )
          }

          override fun isNumber(path: Array<String>, number: EditorNumber): Boolean {
            return this.write(
                parsedValue,
                path,
                number,
                GenericEditorValueWriter {
                    obj: Any?,
                    field: Field,
                    fieldValue: Any?,
                    value: EditorValue? ->
                  if (fieldValue is Number) {
                    setValueUNSAFE(field, obj, value?.getValue())
                    true
                  } else {
                    false
                  }
                },
            )
          }

          override fun isString(path: Array<String>, string: EditorString): Boolean {
            return this.write(
                parsedValue,
                path,
                string,
                GenericEditorValueWriter {
                    obj: Any?,
                    field: Field,
                    fieldValue: Any?,
                    value: EditorValue? ->
                  if (fieldValue is CharSequence) {
                    setValueUNSAFE(field, obj, value?.getValue())
                    true
                  } else {
                    false
                  }
                },
            )
          }
        })
    return true
  }
}
