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

package com.facebook.samples.litho.kotlin.collection

import android.graphics.Color
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.drawableRes
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useCallback
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.collection.LazyList
import com.facebook.yoga.YogaJustify

class SelectionCollectionKComponent : KComponent() {

  private val items = listOf("O-Ren Ishii", "Vernita Green", "Budd", "Elle Driver", "Bill")

  override fun ComponentScope.render(): Component? {
    val selected = useState { setOf<String>() }

    fun isSelected(itemIndex: String): Boolean = selected.value.contains(itemIndex)

    fun isAllSelected(): Boolean = selected.value.size == items.size

    val selectItemClickCallback = useCallback { item: String ->
      selected.update(
          selected
              .value
              .toMutableSet()
              .apply { if (isSelected(item)) remove(item) else add(item) }
              .toSet())
    }

    val selectAllClickCallback = useCallback { _: String ->
      selected.update(if (isAllSelected()) emptySet() else items.toSet())
    }

    return LazyList {
      val isAllSelected = isAllSelected()
      child(deps = arrayOf(isAllSelected)) {
        Selectable(
            item = "Select All", selected = isAllSelected, onRowClick = selectAllClickCallback)
      }

      items.forEach { name ->
        val selected = isSelected(name)
        child(id = name, deps = arrayOf(selected)) {
          Selectable(item = name, selected = selected, onRowClick = selectItemClickCallback)
        }
      }

      child(
          Text(
              selected.value.joinToString(prefix = "Selected: "),
              textColor = Color.DKGRAY,
              style = Style.padding(horizontal = 20.dp, vertical = 10.dp)))
    }
  }
}

class Selectable(val item: String, val selected: Boolean, val onRowClick: (String) -> Unit) :
    KComponent() {

  override fun ComponentScope.render(): Component =
      Row(
          justifyContent = YogaJustify.SPACE_BETWEEN,
          style =
              Style.padding(horizontal = 20.dp, vertical = 10.dp).onClick { onRowClick(item) }) {
        child(Text(item))
        child(
            Image(
                drawableRes(
                    if (selected) android.R.drawable.checkbox_on_background
                    else android.R.drawable.checkbox_off_background)))
      }
}
