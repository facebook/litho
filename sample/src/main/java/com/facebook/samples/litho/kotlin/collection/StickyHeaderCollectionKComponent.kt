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
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.widget.collection.LazyList

class StickyHeaderCollectionKComponent : KComponent() {

  override fun ComponentScope.render(): Component = LazyList {
    child(isSticky = true, component = Text("Sticky Title 1", backgroundColor = Color.WHITE))
    children(items = (0..20), id = { it }) { Text("$it") }
    child(isSticky = true, component = Text("Sticky Title 2", backgroundColor = Color.WHITE))
    children(items = (21..40), id = { it }) { Text("$it") }
    child(isSticky = false, component = Text("Not sticky Title 3", backgroundColor = Color.WHITE))
    children(items = (41..60), id = { it }) { Text("$it") }
    child(isSticky = true, component = Text("Sticky Title 4", backgroundColor = Color.WHITE))
    children(items = (61..80), id = { it }) { Text("$it") }
  }
}
