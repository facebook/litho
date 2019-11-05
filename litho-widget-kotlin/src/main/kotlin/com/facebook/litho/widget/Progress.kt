/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.facebook.litho.ComponentContext

/**
 * Builder function for creating [ProgressSpec] components.
 */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ComponentContext.Progress(
    @ColorInt color: Int,
    indeterminateDrawable: Drawable? = null
): Progress.Builder =
    Progress.create(this)
        .color(color)
        .indeterminateDrawable(indeterminateDrawable)
