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
    private val layoutOutput: LithoLayoutOutput,
    private val paddingSet: Boolean,
    private val backgroundDrawable: Drawable?,
) : ComponentLayout {

  override val x: Int
    get() = layoutOutput.x

  override val y: Int
    get() = layoutOutput.y

  override val width: Int
    get() = layoutOutput.width

  override val height: Int
    get() = layoutOutput.height

  override val paddingTop: Int
    get() = layoutOutput.paddingTop

  override val paddingRight: Int
    get() = layoutOutput.paddingRight

  override val paddingBottom: Int
    get() = layoutOutput.paddingBottom

  override val paddingLeft: Int
    get() = layoutOutput.paddingLeft

  override val isPaddingSet: Boolean
    get() = paddingSet

  override val background: Drawable?
    get() = backgroundDrawable

  override val resolvedLayoutDirection: YogaDirection
    get() = layoutOutput.layoutDirection.toYogaDirection()
}
