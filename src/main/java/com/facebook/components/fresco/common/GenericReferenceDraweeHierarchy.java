/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.fresco.common;

import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.reference.Reference;
import com.facebook.drawee.drawable.AutoRotateDrawable;
import com.facebook.drawee.drawable.DrawableUtils;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;

import static com.facebook.drawee.generic.GenericDraweeHierarchyBuilder.DEFAULT_SCALE_TYPE;

/**
 * A wrapper around {@link com.facebook.drawee.generic.GenericDraweeHierarchy} which correctly
 * manages references.
 */
public class GenericReferenceDraweeHierarchy implements SettableDraweeHierarchy {

  private final GenericDraweeHierarchy mGenericDraweeHierarchy;

  private ComponentContext mContext;

  private Reference<Drawable> mPlaceholderReference;
  private Reference<Drawable> mRetryReference;
  private Reference<Drawable> mFailureReference;
  private Reference<Drawable> mProgressBarReference;
  private Reference<Drawable> mOverlayImageReference;
