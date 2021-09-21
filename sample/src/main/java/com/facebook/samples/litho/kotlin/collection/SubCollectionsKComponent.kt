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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.sections.widget.SubCollection
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.Text

class SubCollectionsKComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val nestedContentVisible = useState { true }

    val header = header()
    val body = body(nestedContentVisible)
    val footer = FooterUtils.getFooter(this)

    return Collection {
      subCollection(header)
      subCollection(body)
      subCollection(footer)
    }
  }

  fun ComponentScope.header(): SubCollection {
    return SubCollection { child(isSticky = true) { Text("Header") } }
  }

  fun ComponentScope.body(nestedContentVisible: State<Boolean>): SubCollection {
    val nestedContent = SubCollection {
      (0..3).forEach { child(id = it) { Text("  Nested Body Item $it") } }
    }
    return SubCollection {
      child {
        Text(
            "${if (nestedContentVisible.value) "-" else "+"} Body",
            style = Style.onClick { nestedContentVisible.update(!nestedContentVisible.value) })
      }
      if (nestedContentVisible.value) subCollection(nestedContent)
    }
  }
}

object FooterUtils {
  fun getFooter(componentScope: ComponentScope): SubCollection {
    with(componentScope) {
      return SubCollection { child { Text("Footer") } }
    }
  }
}
