/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.squareup.javapoet.ClassName;

/**
 * Constants used in {@link SpecModel}s.
 */
public interface ClassNames {
  ClassName OBJECT = ClassName.bestGuess("java.lang.Object");
  ClassName STRING = ClassName.bestGuess("java.lang.String");

  ClassName VIEW = ClassName.bestGuess("android.view.View");
  ClassName DRAWABLE =
      ClassName.bestGuess("android.graphics.drawable.Drawable");

  ClassName ACCESSIBILITY_NODE =
      ClassName.bestGuess("android.support.v4.view.accessibility.AccessibilityNodeInfoCompat");

  ClassName STRING_RES = ClassName.bestGuess("android.support.annotation.StringRes");
  ClassName INT_RES = ClassName.bestGuess("android.support.annotation.IntegerRes");
  ClassName BOOL_RES = ClassName.bestGuess("android.support.annotation.BoolRes");
  ClassName COLOR_RES = ClassName.bestGuess("android.support.annotation.ColorRes");
  ClassName COLOR_INT = ClassName.bestGuess("android.support.annotation.ColorInt");
  ClassName DIMEN_RES = ClassName.bestGuess("android.support.annotation.DimenRes");
  ClassName ATTR_RES = ClassName.bestGuess("android.support.annotation.AttrRes");
  ClassName DRAWABLE_RES = ClassName.bestGuess("android.support.annotation.DrawableRes");
  ClassName ARRAY_RES = ClassName.bestGuess("android.support.annotation.ArrayRes");
  ClassName DIMENSION = ClassName.bestGuess("android.support.annotation.Dimension");
  ClassName PX = ClassName.bestGuess("android.support.annotation.Px");

  ClassName SYNCHRONIZED_POOL =
      ClassName.bestGuess("android.support.v4.util.Pools.SynchronizedPool");

  ClassName LAYOUT_SPEC = ClassName.bestGuess("com.facebook.litho.annotations.LayoutSpec");
  ClassName MOUNT_SPEC = ClassName.bestGuess("com.facebook.litho.annotations.MountSpec");

  ClassName OUTPUT = ClassName.bestGuess("com.facebook.litho.Output");
  ClassName DIFF = ClassName.bestGuess("com.facebook.litho.Diff");
  ClassName SIZE = ClassName.bestGuess("com.facebook.litho.Size");

  ClassName ANIMATION = ClassName.bestGuess("com.facebook.litho.Transition");
