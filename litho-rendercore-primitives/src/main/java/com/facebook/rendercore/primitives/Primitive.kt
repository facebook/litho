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

package com.facebook.rendercore.primitives

import android.graphics.drawable.Drawable
import android.view.View
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.Node
import com.facebook.rendercore.SizeConstraints

/**
 * Primitive is the fundamental building block of a UI component that can be shared across
 * frameworks such as Bloks and Litho. It's two main responsibilities are:
 * - Laying itself out (along with laying out and placing children if applicable)
 * - Optionally, mounting and configuring a [View] or a [Drawable]
 *
 * A Primitive component must be immutable, and not cause side effects.
 *
 * @property layoutBehavior - defines how a Primitive lays out itself and its children (if present)
 * @property mountBehavior - defines how a Primitive mounts and configures a [View] or a [Drawable]
 *   associated with that Primitive
 */
class Primitive(val layoutBehavior: LayoutBehavior, private val mountBehavior: MountBehavior<*>) :
    Node<Any?> {
  /** Lazily creates and returns a [RenderUnit] configured with [mountBehavior]. */
  val renderUnit: PrimitiveRenderUnit<*>
    get() {
      return mountBehavior.renderUnit
    }

  override fun calculateLayout(
      context: LayoutContext<Any?>,
      widthSpec: Int,
      heightSpec: Int
  ): LayoutResult {
    val layoutScope = LayoutScope(context, context.consumePreviousLayoutDataForCurrentNode())
    return with(layoutBehavior) {
      layoutScope
          .layout(SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec))
          .toNodeLayoutResult(widthSpec, heightSpec, renderUnit)
    }
  }
}
