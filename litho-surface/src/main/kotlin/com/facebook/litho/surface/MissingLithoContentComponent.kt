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

package com.facebook.litho.surface

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.EmptyComponent
import com.facebook.litho.KComponent

/**
 * Internal placeholder mounted when [LithoViewModel.getLithoView] or
 * [LithoViewModel.bindLithoViewTo] is called before [LithoViewModel.setLithoContent]. The surface
 * needs *some* root component to bind a [com.facebook.litho.LithoView] to; rendering nothing here
 * keeps the surface visually identical to a deliberately-empty surface, while the structural
 * presence of this component (plus the `LithoSurface:MissingContent` debug event reported from
 * [LithoViewModel] when the placeholder is built) gives developers an attributable signal in
 * tooling.
 *
 * A subsequent [LithoViewModel.setLithoContent] call swaps this placeholder out via
 * [com.facebook.litho.ComponentTree.setRootAsync] — no special handling needed at the call site.
 */
internal class MissingLithoContentComponent(@Suppress("unused") private val surfaceKey: String) :
    KComponent() {
  override fun ComponentScope.render(): Component = EmptyComponent()
}
