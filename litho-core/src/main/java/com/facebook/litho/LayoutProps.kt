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

import androidx.annotation.Px
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

interface LayoutProps {

  fun widthPx(@Px width: Int)

  fun widthPercent(percent: Float)

  fun minWidthPx(@Px minWidth: Int)

  fun maxWidthPx(@Px maxWidth: Int)

  fun minWidthPercent(percent: Float)

  fun maxWidthPercent(percent: Float)

  fun heightPx(@Px height: Int)

  fun heightPercent(percent: Float)

  fun minHeightPx(@Px minHeight: Int)

  fun maxHeightPx(@Px maxHeight: Int)

  fun minHeightPercent(percent: Float)

  fun maxHeightPercent(percent: Float)

  fun layoutDirection(direction: YogaDirection)

  fun alignSelf(alignSelf: YogaAlign)

  fun flex(flex: Float)

  fun flexGrow(flexGrow: Float)

  fun flexShrink(flexShrink: Float)

  fun flexBasisPx(@Px flexBasis: Int)

  fun flexBasisPercent(percent: Float)

  fun aspectRatio(aspectRatio: Float)

  fun positionType(positionType: YogaPositionType)

  fun positionPx(edge: YogaEdge, @Px position: Int)

  fun positionPercent(edge: YogaEdge, percent: Float)

  fun paddingPx(edge: YogaEdge, @Px padding: Int)

  fun paddingPercent(edge: YogaEdge, percent: Float)

  fun marginPx(edge: YogaEdge, @Px margin: Int)

  fun marginPercent(edge: YogaEdge, percent: Float)

  fun marginAuto(edge: YogaEdge)

  fun isReferenceBaseline(isReferenceBaseline: Boolean)

  fun useHeightAsBaseline(useHeightAsBaseline: Boolean)

  fun heightAuto()

  fun widthAuto()

  fun flexBasisAuto()

  /** Used by [DebugLayoutNodeEditor] */
  fun setBorderWidth(edge: YogaEdge, borderWidth: Float)
}
