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

package com.facebook.litho.widget

import android.view.View
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.effects.useEffect
import com.facebook.litho.onCleanup
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator

/**
 * Test-only PrimitiveComponent whose [LayoutBehavior.layout] calls [LayoutScope.useEffect] —
 * producing a layout-phase [com.facebook.litho.Attachable] on
 * [com.facebook.litho.LithoLayoutResult.effects].
 *
 * This is distinct from [com.facebook.litho.ComponentScope.useEffect] (resolve-phase), which is
 * pre-populated into [com.facebook.litho.ReductionState] before reduction starts and is unaffected
 * by the splice path. Layout-phase effects are accumulated per-node during reduction via
 * [com.facebook.litho.LithoReducer.accumulateLayoutResultAttachables] and must be replayed by the
 * splice path's attachable range.
 */
class PrimitiveWithLayoutEffect(
    private val id: Int,
    private val style: Style? = null,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = LayoutEffectBehavior(id),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> View(context) }) {
              bind(id) { view -> onUnbind {} }
            },
        style = style,
    )
  }
}

private class LayoutEffectBehavior(private val id: Int) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    useEffect(id) { onCleanup {} }
    return PrimitiveLayoutResult(
        width = sizeConstraints.maxWidth.coerceAtMost(60),
        height = sizeConstraints.maxHeight.coerceAtMost(40),
    )
  }
}
