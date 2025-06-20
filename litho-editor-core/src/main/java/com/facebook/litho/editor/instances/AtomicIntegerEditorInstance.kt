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
import java.util.concurrent.atomic.AtomicInteger

class AtomicIntegerEditorInstance : Editor {
  override fun read(f: Field, node: Any?): EditorValue {
    val atomicInteger = Reflection.getValueUNSAFE<AtomicInteger>(f, node)
    return if (atomicInteger == null) EditorValue.string("null")
    else EditorValue.number(atomicInteger.get())
  }

  override fun write(f: Field, node: Any?, values: EditorValue): Boolean {
    values.`when`(
        object : EditorValue.DefaultEditorVisitor() {
          override fun isNumber(editor: EditorNumber): Void? {
            // If the value if non-integer, it gets rounded down.
            Reflection.setValueUNSAFE(f, node, AtomicInteger(editor.value.toInt()))
            return null
          }
        })
    return true
  }
}
