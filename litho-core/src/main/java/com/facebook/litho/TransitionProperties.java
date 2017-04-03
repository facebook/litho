/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.List;

import android.annotation.TargetApi;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.util.Property;
import android.view.View;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.facebook.litho.TransitionProperties.PropertyType.ALPHA;
import static com.facebook.litho.TransitionProperties.PropertyType.NONE;
import static com.facebook.litho.TransitionProperties.PropertyType.TRANSLATION_X;
import static com.facebook.litho.TransitionProperties.PropertyType.TRANSLATION_Y;

/**
 * Utility class to hold properties of a View during a {@link Transition}.
 */
@TargetApi(ICE_CREAM_SANDWICH)
public class TransitionProperties {

  @IntDef(flag = true, value = {
          NONE,
          ALPHA,
          TRANSLATION_X,
          TRANSLATION_Y})
  @Retention(RetentionPolicy.SOURCE)
  @interface PropertyType {
    int NONE = 0;
    int ALPHA = 1 << 0;
    int TRANSLATION_X = 1 << 1;
    int TRANSLATION_Y = 1 << 2;
  }

  static void applyProperty(@PropertyType int propertyType, float value, View view) {
    switch (propertyType) {
      case ALPHA:
        view.setAlpha(value);
        break;

      case TRANSLATION_X:
        view.setTranslationX(value);
        break;

      case TRANSLATION_Y:
        view.setTranslationY(value);
        break;
    }
  }

  static PropertySetHolder createPropertySetHolder(int trackedPropertyFlags, View view) {
    return new PropertySetHolder().recordProperties(trackedPropertyFlags, view);
  }

  static List<PropertyChangeHolder> createPropertyChangeHolderList(
      PropertySetHolder start,
      PropertySetHolder end,
      @PropertyType int trackedPropertyFlags) {
    final int maskedStartPropertyFlags = (start.mPropertyFlags & trackedPropertyFlags);
    final int maskedEndPropertyFlags = (end.mPropertyFlags & trackedPropertyFlags);
    if (maskedStartPropertyFlags != maskedEndPropertyFlags) {
      throw new IllegalArgumentException("Start and End masked properties don't match.");
    }
    final List<PropertyChangeHolder> propertiesChangeHolder = new LinkedList<>();

    if (has(trackedPropertyFlags, ALPHA)
        && start.mAlpha != end.mAlpha) {
      propertiesChangeHolder.add(PropertyChangeHolder.create(
          ALPHA,
          start.mAlpha,
          end.mAlpha));
    }
    if (has(trackedPropertyFlags, TRANSLATION_X)) {
      final float startTranslationX = (start.hasLocation() && end.hasLocation())
          ? (start.mLocation[0] - end.mLocation[0])
          : start.mTranslationX;
      if (startTranslationX != end.mTranslationX) {
        propertiesChangeHolder.add(PropertyChangeHolder.create(
            TRANSLATION_X,
            startTranslationX,
            end.mTranslationX));
      }
    }
    if (has(trackedPropertyFlags, TRANSLATION_Y)) {
      final float startTranslationY = (start.hasLocation() && end.hasLocation())
          ? (start.mLocation[1] - end.mLocation[1])
          : start.mTranslationY;
      if (startTranslationY != end.mTranslationY) {
        propertiesChangeHolder.add(PropertyChangeHolder.create(
            TRANSLATION_Y,
            startTranslationY,
            end.mTranslationY));
      }
    }

    return propertiesChangeHolder;
  }

  static Property getViewPropertyFrom(@PropertyType int propertyType) {
    if (Integer.bitCount(propertyType) > 1) {
      throw new IllegalArgumentException("Only one propertyType allowed.");
    }

    switch (propertyType) {
      case ALPHA:
        return View.ALPHA;

      case TRANSLATION_X:
        return View.TRANSLATION_X;

      case TRANSLATION_Y:
        return View.TRANSLATION_Y;

      default:
        throw new IllegalStateException("PropertyType not recognized.");
    }
  }

  private static boolean has(int propertyFlag, @PropertyType int propertyType) {
    return ((propertyFlag & propertyType) != 0);
  }

  public static class PropertySetHolder {

    private static final int UNDEFINED = Integer.MIN_VALUE;

    private @PropertyType int mPropertyFlags = NONE;
    private float mAlpha;
    private float mTranslationX;
    private float mTranslationY;
    // Location of the target View relative to the hosting ComponentView.
    private int[] mLocation = {UNDEFINED, UNDEFINED};

    void set(@PropertyType int propertyType, float value) {
      if (Integer.bitCount(propertyType) != 1) {
        throw new IllegalArgumentException("This method expects only one PropertyType to be set.");
      }

      mPropertyFlags |= propertyType;

      switch (propertyType) {
        case ALPHA:
          mAlpha = value;
          break;

        case TRANSLATION_X:
          mTranslationX = value;
          break;

        case TRANSLATION_Y:
          mTranslationY = value;
          break;

        default:
          throw new IllegalStateException("propertyType not recognized.");
      }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    float get(@PropertyType int propertyType) {
      switch (propertyType) {
        case ALPHA:
          return mAlpha;

        case TRANSLATION_X:
          return mTranslationX;

        case TRANSLATION_Y:
          return mTranslationY;

        default:
          throw new IllegalStateException("Property type not recognized.");
      }
    }

    @PropertyType
    int getPropertyFlags() {
      return mPropertyFlags;
    }

    boolean has(@PropertyType int propertyType) {
      return TransitionProperties.has(mPropertyFlags, propertyType);
    }

    boolean hasLocation() {
      return (mLocation[0] != UNDEFINED);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof PropertySetHolder)) {
        return false;
      }

      final PropertySetHolder v = (PropertySetHolder) o;

      if (mPropertyFlags == v.mPropertyFlags
          && mAlpha == v.mAlpha
          && mTranslationX == v.mTranslationX
          && mTranslationY == v.mTranslationY
          && mLocation[0] == v.mLocation[0]
          && mLocation[1] == v.mLocation[1]) {
        return true;
      }

      return false;
    }

    @Override
    public int hashCode() {
      int result = 17;

      result = 31 * result + mPropertyFlags;
      result = 31 * result + Float.floatToIntBits(mAlpha);
      result = 31 * result + Float.floatToIntBits(mTranslationX);
      result = 31 * result + Float.floatToIntBits(mTranslationY);
      result = 31 * result + mLocation[0];
      result = 31 * result + mLocation[1];

      return result;
    }

    @Override
    public String toString() {
      return "PropertySetHolder " + super.toString()
          + " - alpha: " + mAlpha
          + " - translation_x: " + mTranslationX
          + " - translation_y: " + mTranslationY;
    }

    PropertySetHolder addProperties(PropertySetHolder holder) {
      if ((mPropertyFlags & holder.mPropertyFlags) != 0) {
        throw new IllegalArgumentException("Trying to merge two PropertySetHolder " +
            "with matching flags.");
      }

      if (holder.has(ALPHA)) {
        set(ALPHA, holder.mAlpha);
      }
      if (holder.has(TRANSLATION_X)) {
        set(TRANSLATION_X, holder.mTranslationX);
      }
      if (holder.has(TRANSLATION_Y)) {
        set(TRANSLATION_Y, holder.mTranslationY);
      }

      return this;
    }

    PropertySetHolder recordProperties(@PropertyType int propertyFlags, View view) {
      if (propertyFlags == NONE) {
        throw new IllegalArgumentException("There are no properties flag set.");
      }

      if (TransitionProperties.has(propertyFlags, ALPHA)) {
        set(ALPHA, view.getAlpha());
      }
      if (TransitionProperties.has(propertyFlags, TRANSLATION_X)
          || TransitionProperties.has(propertyFlags, TRANSLATION_Y)) {
        set(TRANSLATION_X, view.getTranslationX());
        set(TRANSLATION_Y, view.getTranslationY());

        mLocation[0] = mLocation[1] = 0;
        getLocationInComponentView(view, mLocation);
      }

      return this;
    }

    void applyProperties(View view) {
      if (has(ALPHA)) {
        applyProperty(ALPHA, mAlpha, view);
      }
      if (has(TRANSLATION_X)) {
        applyProperty(TRANSLATION_X, mTranslationX, view);
      }
      if (has(TRANSLATION_Y)) {
        applyProperty(TRANSLATION_Y, mTranslationY, view);
      }
    }

    private static void getLocationInComponentView(View view, int[] outLocation) {
      if (view.getParent() == null || !(view.getParent() instanceof ComponentHost)) {
        return;
      }

      outLocation[0] += view.getX();
      outLocation[1] += view.getY();

      getLocationInComponentView((View) view.getParent(), outLocation);
    }
  }

  public static class PropertyChangeHolder {
    @PropertyType int propertyType;
    float start;
    float end;

    public static PropertyChangeHolder create(
        @PropertyType int propertyType,
        float start,
        float end) {
      final PropertyChangeHolder holder = new PropertyChangeHolder();
      holder.propertyType = propertyType;
      holder.start = start;
      holder.end = end;

      return holder;
    }
  }
}
