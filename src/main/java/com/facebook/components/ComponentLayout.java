/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.util.SparseArray;

import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import com.facebook.yoga.YogaEdge;

import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.reference.Reference;

import static android.support.annotation.Dimension.DP;

/**
 * Represents a {@link Component}'s computed layout state. The computed bounds will be
 * used by the framework to define the size and position of the component's mounted
 * {@link android.view.View}s and {@link android.graphics.drawable.Drawable}s returned.
 * by {@link ComponentLifecycle#mount(ComponentContext, Object, Component)}.
 *
 * @see ComponentLifecycle#createLayout(ComponentContext, Component, boolean)
