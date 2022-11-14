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

class MapEditorInstance : Editor {

  companion object {
    val instance: MapEditorInstance by lazy { MapEditorInstance() }
  }

  override fun read(f: Field, node: Any?): EditorValue {
    val dataClassInstance =
        Reflection.getValueUNSAFE<Map<Any?, Any?>>(f, node) ?: return EditorValue.string("null")

    val keyToEditorValue: MutableMap<String, EditorValue> = mutableMapOf()
    for ((key, value) in dataClassInstance.entries) {

      val editorValue: EditorValue =
          when (value) {
            null -> EditorValue.string("null")
            else -> EditorRegistry.readValueThatIsNotAField(value.javaClass, value)
                    ?: EditorValue.string(value.javaClass.toString())
          }
      keyToEditorValue[key.toString()] = editorValue
    }
    return EditorValue.shape(keyToEditorValue)
  }

  override fun write(f: Field, node: Any?, values: EditorValue): Boolean {
    val editedParams = (values as? EditorShape)?.value ?: return true

    val oldMap = Reflection.getValueUNSAFE<Map<Any?, Any?>>(f, node) ?: return true

    val newMap = mutableMapOf<Any?, Any?>()
    newMap.putAll(oldMap)

    for ((key, value) in newMap) {

      if (editedParams.containsKey(key.toString())) {
        // There is a new value. Overwrite it.
        val oldValue = oldMap[key] ?: continue
        val newValue = editedParams[key.toString()] ?: EditorValue.string("null")

        val writtenValue =
            EditorRegistry.writeValueThatIsNotAField(oldValue.javaClass, oldValue, newValue)
        if (writtenValue.hasUpdated() == true) {

          newMap[key] = writtenValue.value()
        }
      }
    }
    Reflection.setValueUNSAFE(f, node, newMap)
    return true
  }
}
