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

import android.content.Context
import com.facebook.litho.utils.MeasureUtils
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.MeasureResult

class MeasureScope(val layoutContext: LayoutContext<*>, val previousLayoutData: Any?) {

  val androidContext: Context
    get() = layoutContext.androidContext

  val resourceResolver: ResourceResolver by lazy {
    ResourceResolver(
        androidContext, ResourceCache.getLatest(androidContext.getResources().getConfiguration()))
  }

  fun withAspectRatio(
      widthSpec: Int,
      heightSpec: Int,
      intrinsicWidth: Int,
      intrinsicHeight: Int,
      aspectRatio: Float,
      outputSize: Size
  ) =
      MeasureUtils.measureWithAspectRatio(
          widthSpec, heightSpec, intrinsicWidth, intrinsicHeight, aspectRatio, outputSize)

  fun withEqualSize(widthSpec: Int, heightSpec: Int): MeasureResult =
      MeasureResult.withEqualDimensions(widthSpec, heightSpec, null)

  fun fromSpecs(widthSpec: Int, heightSpec: Int): MeasureResult =
      MeasureResult.fromSpecs(widthSpec, heightSpec)

  fun withExactSize(
      widthSpec: Int,
      heightSpec: Int,
      desiredWidthPx: Int,
      desiredHeightPx: Int
  ): MeasureResult =
      MeasureResult.withDesiredPx(widthSpec, heightSpec, desiredWidthPx, desiredHeightPx)
}
