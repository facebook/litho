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

class MountableLayoutResult
@JvmOverloads
constructor(
    private val _renderUnit: RenderUnit<*>?,
    private val measuredWidth: Int,
    private val measuredHeight: Int,
    private val _layoutData: Any? = null
) : LayoutResult {

  override fun getRenderUnit(): RenderUnit<*>? = _renderUnit

  override fun getLayoutData(): Any? = _layoutData

  override fun getChildrenCount(): Int = 0

  override fun getChildAt(index: Int): LayoutResult {
    throw IllegalArgumentException("A MountableLayoutResult has no children")
  }

  override fun getXForChildAtIndex(index: Int): Int {
    throw IllegalArgumentException("A MountableLayoutResult has no children")
  }

  override fun getYForChildAtIndex(index: Int): Int {
    throw IllegalArgumentException("A MountableLayoutResult has no children")
  }

  override fun getWidth(): Int = measuredWidth

  override fun getHeight(): Int = measuredHeight

  override fun getPaddingTop(): Int = 0

  override fun getPaddingRight(): Int = 0

  override fun getPaddingBottom(): Int = 0

  override fun getPaddingLeft(): Int = 0
}
