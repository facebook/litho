/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

// Changes to View.MeasureSpec have been made to reflect renaming of the class to SizeSpec.
// Portions of View.MeasureSpec which do not have usage in this library have been omitted.

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.IntDef;
import android.view.View;

import com.facebook.yoga.YogaMeasureMode;

/**
 * A SizeSpec encapsulates the layout requirements passed from parent to child.
 * Each SizeSpec represents a requirement for either the width or the height.
 * A SizeSpec is comprised of a size and a mode. There are two possible
 * modes:
 * <dl>
 * <dt>UNSPECIFIED</dt>
 * <dd>
 * The parent has not imposed any constraint on the child. It can be whatever size
 * it wants.
 * </dd>
 *
 * <dt>EXACTLY</dt>
 * <dd>
 * The parent has determined an exact size for the child. The child is going to be
 * given those bounds regardless of how big it wants to be.
 * </dd>
 *
 * SizeSpecs are implemented as ints to reduce object allocation. This class
 * is provided to pack and unpack the &lt;size, mode&gt; tuple into the int.
 */
public class SizeSpec {

  /**
   * Size specification mode: The parent has not imposed any constraint
   * on the child. It can be whatever size it wants.
   */
  public static final int UNSPECIFIED = View.MeasureSpec.UNSPECIFIED;

  /**
   * Size specification mode: The parent has determined an exact size
   * for the child. The child is going to be given those bounds regardless
   * of how big it wants to be.
   */
  public static final int EXACTLY = View.MeasureSpec.EXACTLY;

  /**
   * Size specification mode: The child can be as large as it wants up
   * to the specified size.
   */
  public static final int AT_MOST = View.MeasureSpec.AT_MOST;

  @IntDef({UNSPECIFIED, EXACTLY, AT_MOST})
  @Retention(RetentionPolicy.SOURCE)
  public @interface MeasureSpecMode {}

  /**
   * Creates a size specification based on the supplied size and mode.
   *
   * The mode must always be one of the following:
   * <ul>
   *  <li>{@link com.facebook.litho.SizeSpec#UNSPECIFIED}</li>
   *  <li>{@link com.facebook.litho.SizeSpec#EXACTLY}</li>
   * </ul>
   *
   * <p><strong>Note:</strong> On API level 17 and lower, makeMeasureSpec's
   * implementation was such that the order of arguments did not matter
   * and overflow in either value could impact the resulting MeasureSpec.
   * {@link android.widget.RelativeLayout} was affected by this bug.
   * Apps targeting API levels greater than 17 will get the fixed, more strict
   * behavior.</p>
   *
   * @param size the size of the size specification
   * @param mode the mode of the size specification
   * @return the size specification based on size and mode
   */
  public static int makeSizeSpec(int size, @MeasureSpecMode int mode) {
    return View.MeasureSpec.makeMeasureSpec(size, mode);
  }

  /**
   * Extracts the mode from the supplied size specification.
   *
   * @param sizeSpec the size specification to extract the mode from
   * @return {@link com.facebook.litho.SizeSpec#UNSPECIFIED} or
   *         {@link com.facebook.litho.SizeSpec#EXACTLY}
   */
  public static int getMode(int sizeSpec) {
    return View.MeasureSpec.getMode(sizeSpec);
  }

  /**
   * Extracts the size from the supplied size specification.
   *
   * @param sizeSpec the size specification to extract the size from
   * @return the size in pixels defined in the supplied size specification
   */
  public static int getSize(int sizeSpec) {
    return View.MeasureSpec.getSize(sizeSpec);
  }

  /**
   * Returns a String representation of the specified measure
   * specification.
   *
   * @param sizeSpec the size specification to convert to a String
   * @return a String with the following format: "MeasureSpec: MODE SIZE"
   */
  public static String toString(int sizeSpec) {
    return View.MeasureSpec.toString(sizeSpec);
  }

  /**
   * Resolve a size spec given a preferred size.
   *
   * @param sizeSpec The spec to resolve.
   * @param preferredSize The preferred size.
   * @return The resolved size.
   */
  public static int resolveSize(int sizeSpec, int preferredSize) {
    switch (SizeSpec.getMode(sizeSpec)) {
      case EXACTLY:
        return SizeSpec.getSize(sizeSpec);
      case AT_MOST:
        return Math.min(SizeSpec.getSize(sizeSpec), preferredSize);
      case UNSPECIFIED:
        return preferredSize;
      default:
        throw new IllegalStateException("Unexpected size mode: " + SizeSpec.getMode(sizeSpec));
    }
  }

  public static int makeSizeSpecFromCssSpec(float cssSize, YogaMeasureMode cssMode) {
    final int mode;
    switch (cssMode) {
      case EXACTLY:
        mode = SizeSpec.EXACTLY;
        break;

      case UNDEFINED:
        mode = SizeSpec.UNSPECIFIED;
        break;

      case AT_MOST:
        mode = SizeSpec.AT_MOST;
        break;

      default:
        throw new IllegalArgumentException("Unexpected YogaMeasureMode: " + cssMode);
    }

    return makeSizeSpec(FastMath.round(cssSize), mode);
  }
