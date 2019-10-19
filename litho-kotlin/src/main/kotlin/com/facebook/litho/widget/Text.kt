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

import com.facebook.litho.ComponentContext

/**
 * Temporary builder function for creating [TextSpec] components. In the future it will either be
 * auto-generated or modified to have the final set of parameters.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun ComponentContext.Text(text: CharSequence, textSizeSp: Float = 14f): Text.Builder =
    Text.create(this)
        .text(text)
        .textSizeSp(textSizeSp)
