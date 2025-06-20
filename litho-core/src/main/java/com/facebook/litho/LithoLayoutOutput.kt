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

import android.graphics.Rect
import com.facebook.litho.layout.LayoutDirection
import com.facebook.rendercore.LayoutResult

/** A data structure that holds the result of Litho layout phase. */
sealed interface LithoLayoutOutput {
  val isCachedLayout: Boolean
  val diffNode: DiffNode?
  val x: Int
  val y: Int
  val width: Int
  val height: Int
  val contentWidth: Int
  val contentHeight: Int
  val widthSpec: Int
  val heightSpec: Int
  val lastMeasuredSize: Long
  val layoutData: Any?
  val wasMeasured: Boolean
  val cachedMeasuresValid: Boolean
  val measureHadExceptions: Boolean
  val paddingLeft: Int
  val paddingTop: Int
  val paddingRight: Int
  val paddingBottom: Int
  val borderLeft: Int
  val borderTop: Int
  val borderRight: Int
  val borderBottom: Int
  val contentRenderUnit: LithoRenderUnit?
  val hostRenderUnit: LithoRenderUnit?
  val backgroundRenderUnit: LithoRenderUnit?
  val foregroundRenderUnit: LithoRenderUnit?
  val borderRenderUnit: LithoRenderUnit?
  val delegate: LayoutResult?
  val actualDeferredNodeResult: LithoLayoutResult?
  val adjustedBounds: Rect
  val layoutDirection: LayoutDirection
}
