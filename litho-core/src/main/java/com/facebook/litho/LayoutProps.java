/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;

interface LayoutProps {
  void widthPx(@Px int width);

  void widthPercent(float percent);

  void minWidthPx(@Px int minWidth);

  void maxWidthPx(@Px int maxWidth);

  void minWidthPercent(float percent);

  void maxWidthPercent(float percent);

  void heightPx(@Px int height);

  void heightPercent(float percent);

  void minHeightPx(@Px int minHeight);

  void maxHeightPx(@Px int maxHeight);

  void minHeightPercent(float percent);

  void maxHeightPercent(float percent);

  void layoutDirection(YogaDirection direction);

  void alignSelf(YogaAlign alignSelf);

  void flex(float flex);

  void flexGrow(float flexGrow);

  void flexShrink(float flexShrink);

  void flexBasisPx(@Px int flexBasis);

  void flexBasisPercent(float percent);

  void aspectRatio(float aspectRatio);

  void positionType(@Nullable YogaPositionType positionType);

  void positionPx(YogaEdge edge, @Px int position);

  void positionPercent(YogaEdge edge, float percent);

  void paddingPx(YogaEdge edge, @Px int padding);

  void paddingPercent(YogaEdge edge, float percent);

  void marginPx(YogaEdge edge, @Px int margin);

  void marginPercent(YogaEdge edge, float percent);

  void marginAuto(YogaEdge edge);

  void isReferenceBaseline(boolean isReferenceBaseline);

  void useHeightAsBaseline(boolean useHeightAsBaseline);
}
