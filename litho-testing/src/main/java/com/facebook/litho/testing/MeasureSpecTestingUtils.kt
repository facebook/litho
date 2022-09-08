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

package com.facebook.litho.testing

import android.view.View

/** Shorthand for creating a [View.MeasureSpec.EXACTLY] measure spec. */
fun exactly(px: Int): Int = View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY)

/** Shorthand for creating a [View.MeasureSpec.AT_MOST] measure spec. */
fun atMost(px: Int): Int = View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.AT_MOST)

/** Shorthand for creating a [View.MeasureSpec.UNSPECIFIED] measure spec. */
@JvmOverloads
fun unspecified(px: Int = 0): Int =
    View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.UNSPECIFIED)
