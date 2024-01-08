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

import android.graphics.drawable.Drawable
import com.facebook.rendercore.FastMath
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaNode

class SpecGeneratedComponentLayout(
    private val yogaNode: YogaNode,
    private val paddingSet: Boolean,
    private val background: Drawable?,
) : ComponentLayout {
  override fun getX(): Int = yogaNode.layoutX.toInt()

  override fun getY(): Int = yogaNode.layoutY.toInt()

  override fun getWidth(): Int = yogaNode.layoutWidth.toInt()

  override fun getHeight(): Int = yogaNode.layoutHeight.toInt()

  override fun getPaddingTop(): Int = FastMath.round(yogaNode.getLayoutPadding(YogaEdge.TOP))

  override fun getPaddingRight(): Int = FastMath.round(yogaNode.getLayoutPadding(YogaEdge.RIGHT))

  override fun getPaddingBottom(): Int = FastMath.round(yogaNode.getLayoutPadding(YogaEdge.BOTTOM))

  override fun getPaddingLeft(): Int = FastMath.round(yogaNode.getLayoutPadding(YogaEdge.LEFT))

  @Deprecated("Deprecated in Java") override fun isPaddingSet(): Boolean = paddingSet

  override fun getBackground(): Drawable? = background

  override fun getResolvedLayoutDirection(): YogaDirection = yogaNode.layoutDirection
}
