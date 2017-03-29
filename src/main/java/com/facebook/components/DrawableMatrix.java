/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.ImageView.ScaleType;

/**
 * Static class containing a factory method for creating a matrix to apply to a drawable.
 */
public final class DrawableMatrix extends Matrix {

  private boolean mShouldClipRect;
