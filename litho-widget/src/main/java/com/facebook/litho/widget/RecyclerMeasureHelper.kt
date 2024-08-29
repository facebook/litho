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
@file:JvmName("RecyclerMeasureHelper")

package com.facebook.litho.widget

import com.facebook.litho.SizeSpec
import com.facebook.rendercore.utils.MeasureSpecUtils.atMost
import com.facebook.rendercore.utils.MeasureSpecUtils.exactly
import com.facebook.rendercore.utils.MeasureSpecUtils.unspecified
import java.lang.IllegalStateException

/**
 * Calculates which is the size spec taking into account the [totalPadding]. This is temporarily
 * defined as helper method because we are on the process of migrating from [LegacyRecyclerSpec] to
 * [ExperimentalRecycler]
 */
internal fun maybeGetSpecWithPadding(
    sizeSpec: Int,
    totalPadding: Int,
): Int {
  val originalSize = SizeSpec.getSize(sizeSpec)
  return when (val specMode = SizeSpec.getMode(sizeSpec)) {
    SizeSpec.UNSPECIFIED -> unspecified()
    SizeSpec.AT_MOST -> atMost(maxOf(0, originalSize - totalPadding))
    SizeSpec.EXACTLY -> exactly(maxOf(0, originalSize - totalPadding))
    else -> throw IllegalStateException("Invalid spec mode: $specMode")
  }
}
