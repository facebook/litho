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

import com.facebook.litho.Style
import com.facebook.litho.editor.Editor
import com.facebook.litho.editor.EditorRegistry
import com.facebook.litho.editor.Reflection
import com.facebook.litho.editor.model.EditorShape
import com.facebook.litho.editor.model.EditorValue
import java.lang.reflect.Field

class StyleEditorInstance : Editor {

  companion object {
    val instance: StyleEditorInstance by lazy { StyleEditorInstance() }
  }

  override fun read(f: Field, node: Any?): EditorValue {
    val style = Reflection.getValueUNSAFE<Style?>(f, node) ?: return EditorValue.string("null")

    val resolvedList = mutableMapOf<String, EditorValue>()

    style.forEach { styleItem ->
      val editorValue =
          when (val value = styleItem.value) {
            null -> EditorValue.string("null")
            else -> EditorRegistry.readValueThatIsNotAField(value.javaClass, value)
                    ?: EditorValue.string(value.javaClass.toString())
          }
      resolvedList[styleItem.field.toString()] = editorValue
    }

    return EditorValue.shape(resolvedList)
  }

  override fun write(f: Field, node: Any?, values: EditorValue): Boolean {
    val editedParams = (values as? EditorShape)?.value ?: return true

    val oldStyle = Reflection.getValueUNSAFE<Style?>(f, node) ?: return true

    var newStyle: Style = Style.Companion
    oldStyle.forEach { styleItem ->
      var updated = false

      if (editedParams.containsKey(styleItem.field.toString())) {

        val newStyleItem = copyDataClass(styleItem) ?: return@forEach

        val oldValue = styleItem.value ?: return@forEach
        val newValue = editedParams[styleItem.field.toString()] ?: EditorValue.string("null")

        val writtenValue =
            EditorRegistry.writeValueThatIsNotAField(oldValue.javaClass, oldValue, newValue)

        if (writtenValue.hasUpdated() == true) {

          val field = newStyleItem.javaClass.declaredFields.find { it.name == "value" }
          field ?: return@forEach
          field.isAccessible = true
          field[newStyleItem] = writtenValue.value()
          newStyle = newStyle.plus(newStyleItem)
          updated = true
        }
      }

      if (!updated) {
        newStyle = newStyle.plus(styleItem)
      }
    }
    Reflection.setValueUNSAFE(f, node, newStyle)
    return true
  }

  private inline fun <reified T> copyDataClass(obj: T): T? {
    return Reflection.invoke(obj, "copy")
  }
}
