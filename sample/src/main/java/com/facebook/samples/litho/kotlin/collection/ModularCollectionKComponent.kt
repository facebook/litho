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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.collection.CollectionContainerScope
import com.facebook.litho.widget.collection.LazyList

class ModularCollectionKComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val nestedContentVisible = useState { true }

    return LazyList {
      addHeader()
      addBody(nestedContentVisible)
      addFooter()
    }
  }

  private fun CollectionContainerScope.addHeader() {
    child(isSticky = true, component = Text("Header"))
  }

  private fun CollectionContainerScope.addBody(nestedContentVisible: State<Boolean>) {
    child(
        Text(
            "${if (nestedContentVisible.value) "-" else "+"} Body",
            style = Style.onClick { nestedContentVisible.update(!nestedContentVisible.value) }))
    if (nestedContentVisible.value) {
      (0..3).forEach { child(id = it, component = Text("  Nested Body Item $it")) }
    }
  }

  private fun CollectionContainerScope.addFooter() {
    child(Text("Footer"))
  }
}
