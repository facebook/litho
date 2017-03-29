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

