/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
