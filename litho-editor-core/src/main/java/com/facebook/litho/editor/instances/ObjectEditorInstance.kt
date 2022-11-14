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

class ObjectEditorInstance : Editor {

  companion object {
    val instance: ObjectEditorInstance by lazy { ObjectEditorInstance() }
  }

  override fun read(f: Field, node: Any?): EditorValue {
    val dataClassInstance =
        Reflection.getValueUNSAFE<Any?>(f, node) ?: return EditorValue.string("null")

    val filedNameToEditorValue =
        dataClassInstance.javaClass.declaredFields
            .map { field ->
              field.isAccessible = true
              val editorValue =
                  when (val value = field[dataClassInstance]) {
                    null -> EditorValue.string("null")
                    else -> EditorRegistry.readValueThatIsNotAField(value.javaClass, value)
                            ?: EditorValue.string(value.javaClass.toString())
                  }
              field.name to editorValue
            }
            .toMap()

    return EditorValue.shape(filedNameToEditorValue)
  }

  override fun write(f: Field, node: Any?, values: EditorValue): Boolean {
    val editedParams = (values as? EditorShape)?.value ?: return true

    val oldDataClassInstance = Reflection.getValueUNSAFE<Any?>(f, node) ?: return true
    val newInstance = copyDataClass(oldDataClassInstance)
    for (field in oldDataClassInstance.javaClass.declaredFields) {
      field.isAccessible = true

      if (editedParams.containsKey(field.name)) {
        // There is a new value. Overwrite it.
        val oldValue = field[oldDataClassInstance] ?: continue
        val newValue = editedParams[field.name] ?: EditorValue.string("null")

        val writtenValue =
            EditorRegistry.writeValueThatIsNotAField(oldValue.javaClass, oldValue, newValue)

        if (writtenValue.hasUpdated() == true) {

          field[newInstance] = writtenValue.value()
        }
      }
    }
    Reflection.setValueUNSAFE(f, node, newInstance)
    return true
  }

  private inline fun <reified T> copyDataClass(obj: T): T {
    return Reflection.invoke(obj, "copy") ?: obj
  }
}
