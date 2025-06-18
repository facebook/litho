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

import android.graphics.drawable.ColorDrawable
import com.facebook.litho.drawable.ComparableColorDrawable
import com.facebook.litho.editor.Editor
import com.facebook.litho.editor.Reflection
import com.facebook.litho.editor.model.EditorNumber
import com.facebook.litho.editor.model.EditorValue
import java.lang.reflect.Field

class ComparableColorDrawableEditorInstance : Editor {
  override fun read(f: Field, node: Any?): EditorValue {
    val colorDrawable = Reflection.getValueUNSAFE<ColorDrawable>(f, node)
    return if (colorDrawable == null) EditorValue.string("null")
    else EditorValue.color(colorDrawable.color)
  }

  override fun write(f: Field, node: Any?, values: EditorValue): Boolean {
    val comparableColorDrawable: ComparableColorDrawable =
        ComparableColorDrawable.create((values as EditorNumber).value.toInt())
    Reflection.setValueUNSAFE(f, node, comparableColorDrawable)
    return true
  }
}
