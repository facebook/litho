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

package com.facebook.litho.effects

import com.facebook.litho.CleanupFunc
import com.facebook.litho.LithoExtraContextForLayoutScope
import com.facebook.litho.USE_EFFECT_NO_DEPS_ERROR
import com.facebook.litho.UseEffectAttachable
import com.facebook.rendercore.primitives.LayoutScope

/**
 * Registers a callback to perform side-effects from the Layout Behaviour of a Primitive Component.
 *
 * The [onAttach] callback will be invoked when the owning Component first becomes part of a
 * committed layout on the ComponentTree on the main thread. It should return a
 * [com.facebook.litho.CleanupFunc] callback to be invoked when the owning Component is detached,
 * e.g. the ComponentTree is released or the Component is no longer part of the tree.
 *
 * Additionally, any time the list of [deps] changes between layouts, the existing onAttach's
 * cleanup callback will be invoked, and the new onAttach callback will be invoked.
 *
 * [deps] should contain any props or state your attach/cleanup callbacks use. For example, if
 * you're using useEffect to subscribe to a data store based on an id you get as a prop, [deps]
 * should include that id so that if the id changes, we will unsubscribe from the old id and
 * subscribe with the new id.
 */
fun LayoutScope.useEffect(vararg deps: Any?, onAttach: () -> CleanupFunc?) {
  with(extraContext as LithoExtraContextForLayoutScope) {
    addEffect(UseEffectAttachable(generateEffectId(), deps, onAttach))
  }
}

@Deprecated(USE_EFFECT_NO_DEPS_ERROR, level = DeprecationLevel.ERROR)
@Suppress("unused", "UNUSED_PARAMETER")
fun LayoutScope.useEffect(onAttach: () -> CleanupFunc?): Unit = error(USE_EFFECT_NO_DEPS_ERROR)
