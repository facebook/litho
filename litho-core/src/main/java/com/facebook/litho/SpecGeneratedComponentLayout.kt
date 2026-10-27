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
import com.facebook.yoga.YogaDirection

class SpecGeneratedComponentLayout(
    /**
     * Its position in its parent; while it is measured, before anything places it, where its own
     * engine positioned it.
     */
    override val x: Int,
    /** Its vertical position, as [x] is its horizontal one. */
    override val y: Int,
    private val measureOutput: LithoMeasureOutput,
    private val paddingSet: Boolean,
    private val backgroundDrawable: Drawable?,
) : ComponentLayout {

  override val width: Int
    get() = measureOutput.width

  override val height: Int
    get() = measureOutput.height

  override val paddingTop: Int
    get() = measureOutput.paddingTop

  override val paddingRight: Int
    get() = measureOutput.paddingRight

  override val paddingBottom: Int
    get() = measureOutput.paddingBottom

  override val paddingLeft: Int
    get() = measureOutput.paddingLeft

  override val isPaddingSet: Boolean
    get() = paddingSet

  override val background: Drawable?
    get() = backgroundDrawable

  override val resolvedLayoutDirection: YogaDirection
    get() = measureOutput.layoutDirection.toYogaDirection()
}
