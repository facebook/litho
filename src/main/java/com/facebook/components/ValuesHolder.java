// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.List;

import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.support.annotation.IntDef;
import android.view.View;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.facebook.components.ValuesHolder.ValueType.ALPHA;
import static com.facebook.components.ValuesHolder.ValueType.NONE;
import static com.facebook.components.ValuesHolder.ValueType.TRANSLATION_X;
import static com.facebook.components.ValuesHolder.ValueType.TRANSLATION_Y;

/**
 * Utility class to hold values of a View during a {@link Transition}.
 */
@TargetApi(ICE_CREAM_SANDWICH)
class ValuesHolder {

  @IntDef(flag = true, value = {
          NONE,
          ALPHA,
          TRANSLATION_X,
          TRANSLATION_Y})
  @Retention(RetentionPolicy.SOURCE)
  @interface ValueType {
    int NONE = 0;
    int ALPHA = 1 << 0;
    int TRANSLATION_X = 1 << 1;
    int TRANSLATION_Y = 1 << 2;
  }

  private @ValueType int mValuesFlag = NONE;
  private float mAlpha;
  private float mTranslationX;
  private float mTranslationY;

  void set(@ValueType int valueType, float value) {
    if (Integer.bitCount(valueType) != 1) {
      throw new IllegalArgumentException("This method expects only one ValueType to be set.");
    }

    mValuesFlag |= valueType;

    switch (valueType) {
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
        throw new IllegalStateException("Value type not recognized.");
    }
  }

  @ValueType int getValuesFlag() {
    return mValuesFlag;
  }

  boolean has(@ValueType int valueType) {
    return has(mValuesFlag, valueType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ValuesHolder)) {
      return false;
    }

    final ValuesHolder v = (ValuesHolder) o;

    if (mValuesFlag == v.mValuesFlag
        && mAlpha == v.mAlpha
        && mTranslationX == v.mTranslationX
        && mTranslationY == v.mTranslationY) {
      return true;
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;

    result = 31 * result + mValuesFlag;
    result = 31 * result + Float.floatToIntBits(mAlpha);
    result = 31 * result + Float.floatToIntBits(mTranslationX);
    result = 31 * result + Float.floatToIntBits(mTranslationY);

    return result;
  }

  @Override
  public String toString() {
    return "ValuesHolder " + super.toString()
        + " - alpha: " + mAlpha
        + " - translation_x: " + mTranslationX
        + " - translation_y: " + mTranslationY;
  }

  /**
   * Apply the previously set values to the given View.
   */
  void applyValuesTo(View view) {
    if (has(ALPHA)) {
      view.setAlpha(mAlpha);
    }
    if (has(TRANSLATION_X)) {
      view.setTranslationX(mTranslationX);
    }
    if (has(TRANSLATION_Y)) {
      view.setTranslationY(mTranslationY);
    }
  }

  ValuesHolder addValues(ValuesHolder holder) {
    if ((mValuesFlag & holder.mValuesFlag) != 0) {
      throw new IllegalArgumentException("Trying to merge two ValuesHolders with matching flags.");
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

  ValuesHolder recordValues(@ValueType int valuesFlag, View view) {
    if (valuesFlag == NONE) {
      throw new IllegalArgumentException("There are no properties flag set.");
    }

    if (has(valuesFlag, ALPHA)) {
      set(ALPHA, view.getAlpha());
    }
    if (has(valuesFlag, TRANSLATION_X)) {
      set(TRANSLATION_X, view.getTranslationX());
    }
    if (has(valuesFlag, TRANSLATION_Y)) {
      set(TRANSLATION_Y, view.getTranslationY());
    }

    return this;
  }

  static PropertyValuesHolder[] createPropertyValuesHolders(
      ValuesHolder start,
      ValuesHolder end,
      @ValueType int valuesFlagMask) {
    final int maskedStartValuesFlags = (start.mValuesFlag & valuesFlagMask);
    final int maskedEndValuesFlags = (end.mValuesFlag & valuesFlagMask);
    if (maskedStartValuesFlags != maskedEndValuesFlags) {
      throw new IllegalArgumentException("Start and End masked values don't match.");
    }

    final List<PropertyValuesHolder> values = new LinkedList<>();

    if (has(valuesFlagMask, ALPHA)
        && start.mAlpha != end.mAlpha) {
      values.add(PropertyValuesHolder.ofFloat(
          View.ALPHA,
          start.mAlpha,
          end.mAlpha));
    }
    if (has(valuesFlagMask, TRANSLATION_X)
        && start.mTranslationX != end.mTranslationX) {
      values.add(PropertyValuesHolder.ofFloat(
          View.TRANSLATION_X,
          start.mTranslationX,
          end.mTranslationX));
    }
    if (has(valuesFlagMask, TRANSLATION_Y)
        && start.mTranslationY != end.mTranslationY) {
      values.add(PropertyValuesHolder.ofFloat(
          View.TRANSLATION_Y,
          start.mTranslationY,
          end.mTranslationY));
    }

    return values.toArray(new PropertyValuesHolder[values.size()]);
  }

  private static boolean has(int valuesFlag, @ValueType int valueType) {
    return ((valuesFlag & valueType) != 0);
  }
}
