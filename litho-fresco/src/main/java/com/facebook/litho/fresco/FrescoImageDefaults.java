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

package com.facebook.litho.fresco;

import android.graphics.PointF;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;

public class FrescoImageDefaults {

  public static final ScalingUtils.ScaleType DEFAULT_ACTUAL_IMAGE_SCALE_TYPE =
      GenericDraweeHierarchyBuilder.DEFAULT_ACTUAL_IMAGE_SCALE_TYPE;
  public static final int DEFAULT_FADE_DURATION =
      GenericDraweeHierarchyBuilder.DEFAULT_FADE_DURATION;
  public static final ScalingUtils.ScaleType DEFAULT_SCALE_TYPE =
      GenericDraweeHierarchyBuilder.DEFAULT_SCALE_TYPE;
  public static final float DEFAULT_IMAGE_ASPECT_RATION = 1f;
  public static final PointF DEFAULT_PLACEHOLDER_IMAGE_FOCUS_POINT = new PointF(0.5f, 0.5f);
}
