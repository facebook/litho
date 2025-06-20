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
import com.facebook.litho.editor.model.EditorBool
import com.facebook.litho.editor.model.EditorValue
import java.lang.reflect.Field

class BoolEditorInstance : Editor {
  override fun read(f: Field, node: Any?): EditorValue {
    // If you use Boolean here it causes an exception when attempting to do implicit conversion to
    // boolean
    val b = Reflection.getValueUNSAFE<Any>(f, node)
    return if (b == null) EditorValue.string("null") else EditorValue.bool(b as Boolean)
  }

  override fun write(f: Field, node: Any?, values: EditorValue): Boolean {
    values.`when`(
        object : EditorValue.DefaultEditorVisitor() {
          override fun isBool(bool: EditorBool): Void? {
            Reflection.setValueUNSAFE<Any>(f, node, bool.value)
            return null
          }
        })
    return true
  }
}
