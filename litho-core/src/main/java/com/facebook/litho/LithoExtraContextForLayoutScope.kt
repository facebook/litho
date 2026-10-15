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

import com.facebook.litho.layout.LayoutDirection
import com.facebook.rendercore.primitives.ResolvedLayoutDirectionProvider

class LithoExtraContextForLayoutScope
internal constructor(
    val componentContext: ComponentContext,
    val layoutDirection: LayoutDirection,
    val layoutVersion: Int,
) : ResolvedLayoutDirectionProvider {

  override val isResolvedLayoutDirectionRtl: Boolean?
    get() =
        when {
          layoutDirection.isRTL -> true
          layoutDirection.isLTR -> false
          else -> null
        }

  internal var effects: MutableList<Attachable>? = null
    private set

  internal fun addEffect(entry: Attachable) {
    (effects ?: mutableListOf<Attachable>().apply { effects = this }).add(entry)
  }

  internal fun generateEffectId(): Any {
    val index = effects?.size ?: 0
    return HookGlobalKey(componentContext.globalKey, index, RenderScope.Layout)
  }
}

/**
 * Extension property to get the layout version from [LayoutScope].
 *
 * This version is inherited from the parent tree's layout pass and should be used when performing
 * nested tree layouts to ensure version consistency across the tree hierarchy.
 */
val com.facebook.rendercore.primitives.LayoutScope.layoutVersion: Int
  get() = (extraContext as LithoExtraContextForLayoutScope).layoutVersion
