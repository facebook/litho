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

package com.facebook.rendercore

import android.view.View
import java.util.ArrayList

open class DefaultNode @JvmOverloads constructor(val layoutParams: YogaProps? = null) : Node<Any?> {

  private val _children: MutableList<Node<*>> = ArrayList()
  private var _renderUnit: RenderUnit<*>? = null

  val childrenCount: Int
    get() = _children.size

  fun getChildAt(index: Int): Node<*> = _children[index]

  val children: List<Node<*>>
    get() = _children

  fun addChild(node: Node<*>) {
    _children.add(node)
  }

  fun setRenderUnit(renderUnit: RenderUnit<*>?) {
    _renderUnit = renderUnit
  }

  fun getRenderUnit(layoutContext: LayoutContext<*>?): RenderUnit<*>? = _renderUnit

  override fun calculateLayout(
      context: LayoutContext<Any?>,
      widthSpec: Int,
      heightSpec: Int
  ): LayoutResult {
    val renderUnit = getRenderUnit(context)
    return if (renderUnit != null) {
      MountableLayoutResult(
          renderUnit,
          widthSpec,
          heightSpec,
          if (View.MeasureSpec.getMode(widthSpec) == View.MeasureSpec.EXACTLY)
              View.MeasureSpec.getSize(widthSpec)
          else 0,
          if (View.MeasureSpec.getMode(heightSpec) == View.MeasureSpec.EXACTLY)
              View.MeasureSpec.getSize(heightSpec)
          else 0)
    } else {
      emptyLayout
    }
  }

  companion object {
    private val emptyLayout: LayoutResult =
        object : LayoutResult {
          override fun getRenderUnit(): RenderUnit<*>? = null

          override fun getLayoutData(): Any? = null

          override fun getChildrenCount(): Int = 0

          override fun getChildAt(index: Int): LayoutResult {
            throw UnsupportedOperationException("Empty LayoutResult has no children.")
          }

          override fun getXForChildAtIndex(index: Int): Int = 0

          override fun getYForChildAtIndex(index: Int): Int = 0

          override fun getWidth(): Int = 0

          override fun getHeight(): Int = 0

          override fun getPaddingTop(): Int = 0

          override fun getPaddingRight(): Int = 0

          override fun getPaddingBottom(): Int = 0

          override fun getPaddingLeft(): Int = 0

          override fun getWidthSpec(): Int = 0

          override fun getHeightSpec(): Int = 0
        }
  }
}
