/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.drawableRes
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.useCallback
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.Image
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaJustify

class SelectionCollectionKComponent : KComponent() {

  val items = listOf("O-Ren Ishii", "Vernita Green", "Budd", "Elle Driver", "Bill")

  override fun ComponentScope.render(): Component? {
    val selected = useState { setOf<Int>() }

    fun isSelected(itemIndex: Int): Boolean = selected.value.contains(itemIndex)

    fun isAllSelected(): Boolean = selected.value.size == items.size

    val selectItemClickCallback = useCallback { itemIndex: Int ->
      selected.update(
          selected
              .value
              .toMutableSet()
              .apply { if (isSelected(itemIndex)) remove(itemIndex) else add(itemIndex) }
              .toSet())
    }

    val selectAllClickCallback = useCallback {
      selected.update(if (isAllSelected()) emptySet() else (0..items.lastIndex).toSet())
    }

    return Collection {
      val isAllSelected = isAllSelected()
      child(deps = arrayOf(isAllSelected)) {
        Selectable(
            text = "Select All",
            selected = isAllSelected,
            onClick = { selectAllClickCallback.current() })
      }

      items.forEachIndexed { index, name ->
        val selected = isSelected(index)
        child(id = name, deps = arrayOf(selected)) {
          Selectable(
              text = name,
              selected = selected,
              onClick = { selectItemClickCallback.current(index) })
        }
      }

      child {
        Text(
            selected.value.joinToString(prefix = "Selected: ") { items[it] },
            textColor = Color.DKGRAY,
            style = Style.padding(horizontal = 20.dp, vertical = 10.dp))
      }
    }
  }
}

class Selectable(val text: String, val selected: Boolean, val onClick: (ClickEvent) -> Unit) :
    KComponent() {

  override fun ComponentScope.render(): Component? {
    return Row(
        justifyContent = YogaJustify.SPACE_BETWEEN,
        style = Style.padding(horizontal = 20.dp, vertical = 10.dp).onClick(onClick)) {
      child(Text(text))
      child(
          Image(
              drawableRes(
                  if (selected) android.R.drawable.checkbox_on_background
                  else android.R.drawable.checkbox_off_background)))
    }
  }
}
