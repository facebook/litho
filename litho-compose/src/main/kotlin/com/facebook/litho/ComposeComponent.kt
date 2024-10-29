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

package com.facebook.litho

import com.facebook.primitive.compose.ComposableWithDeps
import com.facebook.primitive.compose.ComposePrimitive

/**
 * A Litho component that renders Jetpack Compose tree.
 *
 * @property composable The [ComposableWithDeps] that will be rendered by this component. Use
 *   [useComposable] hook to create one.
 * @property contentType The content type for this component. The content recycling and updates of
 *   components of the same type can be performed more efficiently. It's a similar concept to
 *   contentType in Compose LazyColumn and RecyclerView's itemViewType.
 * @property style The style to apply to the component.
 */
class ComposeComponent(
    private val composable: ComposableWithDeps,
    private val contentType: Any,
    private val style: Style? = null,
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        primitive =
            ComposePrimitive(
                id = createPrimitiveId(), contentType = contentType, composable = composable),
        style = style)
  }
}
