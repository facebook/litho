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

package com.facebook.samples.litho.documentation.treeprops

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.TreePropProvider
import com.facebook.litho.core.height
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp

// start_example
class RootKComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    return TreePropProvider(LogContext::class.java to LogContext("root")) {
      Column() {
        child(LeafKComponent())
        child(LazyList(style = Style.height(500.dp)) { child(TopGroupKComponent()) })
      }
    }
  }
}
// end_example
