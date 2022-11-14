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
import com.facebook.litho.editor.EditorRegistry
import com.facebook.litho.editor.Reflection
import com.facebook.litho.editor.model.EditorShape
import com.facebook.litho.editor.model.EditorValue
import java.lang.reflect.Field

class ListEditorInstance : Editor {

  override fun read(f: Field, node: Any?): EditorValue {
    val list = Reflection.getValueUNSAFE<List<Any?>?>(f, node) ?: return EditorValue.string("null")

    val resolvedList =
        list.map {
          when (it) {
            null -> EditorValue.string("null")
            else -> EditorRegistry.readValueThatIsNotAField(it.javaClass, it)
                    ?: EditorValue.string(it.javaClass.toString())
          }
        }

    return EditorValue.array(resolvedList)
  }

  override fun write(f: Field, node: Any?, values: EditorValue): Boolean {
    val editedValues = (values as? EditorShape?)?.value ?: return true

    val oldList = Reflection.getValueUNSAFE<List<Any?>?>(f, node) ?: return true

    val newList = mutableListOf<Any?>()
    newList.addAll(oldList)

    for (index in newList.indices) {
      if (editedValues.containsKey(index.toString())) {
        val oldValue = oldList[index] ?: continue // Overwriting null not supported
        val newValue = editedValues[index.toString()] ?: EditorValue.string("null")
        val writtenValue =
            EditorRegistry.writeValueThatIsNotAField(oldValue.javaClass, oldValue, newValue)
        if (writtenValue.hasUpdated() == true) {
          newList[index] = writtenValue.value()
        }
      }
    }

    Reflection.setValueUNSAFE(f, node, newList)
    return true
  }
}
