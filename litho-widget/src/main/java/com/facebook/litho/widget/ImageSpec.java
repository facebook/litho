/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.widget.ImageView.ScaleType;

import com.facebook.R;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.DrawableMatrix;
import com.facebook.litho.MatrixDrawable;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.reference.Reference;
import com.facebook.litho.reference.ResourceDrawableReference;
import com.facebook.litho.utils.MeasureUtils;

import static com.facebook.litho.SizeSpec.UNSPECIFIED;

/**
 * A component that is able to display drawable resources. It takes a drawable
 * resource ID as prop.
 */
@MountSpec(isPureRender = true, poolSize = 30)
class ImageSpec {

