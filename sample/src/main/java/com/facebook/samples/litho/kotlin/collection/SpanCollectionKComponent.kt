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
import com.facebook.litho.widget.collection.LazyGrid

class SpanCollectionKComponent : KComponent() {

  override fun ComponentScope.render(): Component {

    return LazyGrid(
        columns = 4,
    ) {
      child(isFullSpan = true, component = Text("Full Span", backgroundColor = Color.WHITE))
      (0..20).forEach { child(id = it, component = Text("$it")) }
      child(spanSize = 2, component = Text("Span 2", backgroundColor = Color.WHITE))
      (21..40).forEach { child(id = it, component = Text("$it")) }
      child(spanSize = 3, component = Text("Span 2", backgroundColor = Color.WHITE))
      (41..60).forEach { child(id = it, component = Text("$it")) }
      child(Text("Default Span", backgroundColor = Color.WHITE))
      (61..80).forEach { child(id = it, component = Text("$it")) }
    }
  }
}
