/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import java.lang.reflect.Field;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

/**
 * A utility class to compare Drawable instances.
 */
public class DrawableUtils {

  private static Field sLayerDrawableState;
  private static Field sLayerChildren;
  private static Field sInsetL;
  private static Field sInsetT;
  private static Field sInsetR;
  private static Field sInsetB;
  private static boolean sReflectionInitialized = false;

  // We need reflection here since when comparing LayerDrawable instances there is no public
  // API to access the inset of the inner layers. Not checking inner layers insets was causing
  // the bug described in t9735777. This will not be necessary anymore as soon as we move everything
  // back to Reference API. TODO t9745247.
  static {
    try {
      Class layerStateClass = Class.forName("android.graphics.drawable.LayerDrawable$LayerState");
      Class childDrawableClass =
          Class.forName("android.graphics.drawable.LayerDrawable$ChildDrawable");

      sLayerDrawableState = LayerDrawable.class.getDeclaredField("mLayerState");
      sLayerDrawableState.setAccessible(true);

      sLayerChildren = layerStateClass.getDeclaredField("mChildren");
      sLayerChildren.setAccessible(true);

      sInsetL = childDrawableClass.getDeclaredField("mInsetL");
      sInsetL.setAccessible(true);

      sInsetT = childDrawableClass.getDeclaredField("mInsetT");
      sInsetT.setAccessible(true);

      sInsetR = childDrawableClass.getDeclaredField("mInsetR");
      sInsetR.setAccessible(true);

      sInsetB = childDrawableClass.getDeclaredField("mInsetB");
      sInsetB.setAccessible(true);

      sReflectionInitialized = true;
    } catch (Exception e) {
      sReflectionInitialized = false;
    }
  }

  /**
   * Returns whether two drawables can be considered equal. Currently only supports
   * {@link LayerDrawable}, {@link ColorDrawable} and {@link Drawable} with the same constant
   * state.
   */
  public static boolean areDrawablesEqual(Drawable drawable, Drawable otherDrawable) {
    if (drawable == otherDrawable) {
      return true;
    }

    if (drawable == null && otherDrawable == null) {
      return true;
    }

    if (drawable == null || otherDrawable == null) {
      return false;
    }

    if (drawable.getClass() != otherDrawable.getClass()) {
      return false;
    }

    if (drawable instanceof ColorDrawable) {
      final ColorDrawable colorDrawable = (ColorDrawable) drawable;
      final ColorDrawable otherColorDrawable = (ColorDrawable) otherDrawable;

      return areColorDrawablesEqual(colorDrawable, otherColorDrawable);
    } else if (drawable instanceof LayerDrawable) {
      final LayerDrawable layerDrawable = (LayerDrawable) drawable;
      final LayerDrawable otherLayerDrawable = (LayerDrawable) otherDrawable;

      return areLayerDrawablesEqual(layerDrawable, otherLayerDrawable);
    }

    return drawable.getConstantState() == otherDrawable.getConstantState();
  }

  private static boolean areColorDrawablesEqual(
      ColorDrawable colorDrawable,
      ColorDrawable otherColorDrawable) {
    if (!(colorDrawable.getColor() == otherColorDrawable.getColor())) {
      return false;
    }

    if (!(colorDrawable.getOpacity() == otherColorDrawable.getOpacity())) {
      return false;
    }

    if (!(colorDrawable.getAlpha() == otherColorDrawable.getAlpha())) {
      return false;
    }

    return true;
  }

  private static boolean areLayerDrawablesEqual(
      LayerDrawable layerDrawable,
      LayerDrawable otherLayerDrawable) {

    if (!sReflectionInitialized) {
      return false;
    }

    final Object[] children;
    final Object[] otherChildren;

    try {
      Object layerState = sLayerDrawableState.get(layerDrawable);
      children = (Object[]) sLayerChildren.get(layerState);

      Object otherState = sLayerDrawableState.get(otherLayerDrawable);
      otherChildren = (Object[]) sLayerChildren.get(otherState);
    } catch (IllegalAccessException e) {
      return false;
    }

    if (children == null || otherChildren == null) {
       return false;
    }

    if (layerDrawable.getNumberOfLayers() != otherLayerDrawable.getNumberOfLayers()) {
      return false;
    }

    for (int i = 0, size = layerDrawable.getNumberOfLayers(); i < size; i++) {
      final Drawable firstDrawable = layerDrawable.getDrawable(i);
      final Drawable secondDrawable = otherLayerDrawable.getDrawable(i);

      if (!areDrawablesEqual(firstDrawable, secondDrawable)) {
        return false;
      }

      try {
        if (!compareInsets(children[i], otherChildren[i])) {
          return false;
        }
      } catch (IllegalAccessException e) {
        return false;
      }
    }

    return true;
  }

  private static boolean compareInsets(
      Object child,
      Object otherChild) throws IllegalAccessException {

    final int childInsetL = sInsetL.getInt(child);
    final int otherChildInsetL = sInsetL.getInt(otherChild);
    if (childInsetL != otherChildInsetL) {
      return false;
    }

    final int childInsetT = sInsetT.getInt(child);
    final int otherChildInsetT = sInsetT.getInt(otherChild);
    if (childInsetT != otherChildInsetT) {
      return false;
    }

    final int childInsetR = sInsetR.getInt(child);
    final int otherChildInsetR = sInsetR.getInt(otherChild);
    if (childInsetR != otherChildInsetR) {
      return false;
    }

    final int childInsetB = sInsetB.getInt(child);
    final int otherChildInsetB = sInsetB.getInt(otherChild);
    if (childInsetB != otherChildInsetB) {
      return false;
    }

    return true;
  }
}
