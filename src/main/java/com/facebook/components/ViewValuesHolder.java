// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.view.View;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

/**
 * Utility class to hold values of a View during a {@link Transition}.
 */
@TargetApi(ICE_CREAM_SANDWICH)
class ViewValuesHolder {

  private static final int NONE = 0;
  private static final int TRANSLATION_X = 1 << 0;
  private static final int TRANSLATION_Y = 1 << 1;
  private static final int SCALE_X = 1 << 2;
  private static final int SCALE_Y = 1 << 3;
  private static final int ROTATION = 1 << 4;
  private static final int ROTATION_X = 1 << 5;
  private static final int ROTATION_Y = 1 << 6;
  private static final int ALPHA = 1 << 7;

  private int mSetFlags = NONE;

  private float mTranslationX;
  private float mTranslationY;
  private float mScaleX;
  private float mScaleY;
  private float mRotation;
  private float mRotationX;
  private float mRotationY;
  private float mAlpha;

  void setTranslationX(float translationX) {
    mSetFlags |= TRANSLATION_X;
    mTranslationX = translationX;
  }

  void setTranslationY(float translationY) {
    mSetFlags |= TRANSLATION_Y;
    mTranslationY = translationY;
  }

  void setScaleX(float scaleX) {
    mSetFlags |= SCALE_X;
    mScaleX = scaleX;
  }

  void setScaleY(float scaleY) {
    mSetFlags |= SCALE_Y;
    mScaleY = scaleY;
  }

  void setRotation(float rotation) {
    mSetFlags |= ROTATION;
    mRotation = rotation;
  }

  void setRotationX(float rotationX) {
    mSetFlags |= ROTATION_X;
    mRotationX = rotationX;
  }

  void setRotationY(float rotationY) {
    mSetFlags |= ROTATION_Y;
    mRotationY = rotationY;
  }

  void setAlpha(float alpha) {
    mSetFlags |= ALPHA;
    mAlpha = alpha;
  }

  /**
   * Apply the previously set values to the given View.
   */
  void applyValuesTo(View view) {
    if ((mSetFlags & TRANSLATION_X) != 0) {
      view.setTranslationX(mTranslationX);
    }
    if ((mSetFlags & TRANSLATION_Y) != 0) {
      view.setTranslationY(mTranslationY);
    }
    if ((mSetFlags & SCALE_X) != 0) {
      view.setScaleX(mScaleX);
    }
    if ((mSetFlags & SCALE_Y) != 0) {
      view.setScaleY(mScaleY);
    }
    if ((mSetFlags & ROTATION) != 0) {
      view.setRotation(mRotation);
    }
    if ((mSetFlags & ROTATION_X) != 0) {
      view.setRotationX(mRotationX);
    }
    if ((mSetFlags & ROTATION_Y) != 0) {
      view.setRotationY(mRotationY);
    }
    if ((mSetFlags & ALPHA) != 0) {
      view.setAlpha(mAlpha);
    }
  }

  /**
   * Return a new ViewValuesHolder instance matching the same property types set on the current one,
   * but the values are populated from the given View.
   */
  ViewValuesHolder createMatchingValuesFrom(View view) {
    if (mSetFlags == NONE) {
      throw new IllegalStateException("There are no properties set");
    }

    final ViewValuesHolder matchingValuesHolder = new ViewValuesHolder();

    if ((mSetFlags & TRANSLATION_X) != 0) {
      matchingValuesHolder.setTranslationX(view.getTranslationX());
    }
    if ((mSetFlags & TRANSLATION_Y) != 0) {
      matchingValuesHolder.setTranslationY(view.getTranslationY());
    }
    if ((mSetFlags & SCALE_X) != 0) {
      matchingValuesHolder.setScaleX(view.getScaleX());
    }
    if ((mSetFlags & SCALE_Y) != 0) {
      matchingValuesHolder.setScaleY(view.getScaleY());
    }
    if ((mSetFlags & ROTATION) != 0) {
      matchingValuesHolder.setRotation(view.getRotation());
    }
    if ((mSetFlags & ROTATION_X) != 0) {
      matchingValuesHolder.setRotationX(view.getRotationX());
    }
    if ((mSetFlags & ROTATION_Y) != 0) {
      matchingValuesHolder.setRotationY(view.getRotationY());
    }
    if ((mSetFlags & ALPHA) != 0) {
      matchingValuesHolder.setAlpha(view.getAlpha());
    }

    return matchingValuesHolder;
  }

  /**
   * Create an array of {@link PropertyValuesHolder} given a start and end ViewValuesHolder.
   * The start and end ViewValuesHolder instances must have matching value types set.
   */
  static PropertyValuesHolder[] createValueHolders(
      ViewValuesHolder start,
      ViewValuesHolder end) {
    if (start.mSetFlags != end.mSetFlags) {
      throw new IllegalArgumentException("Start and end ValuesHolder have non matching " +
          "properties.");
    }

    final PropertyValuesHolder[] values =
        new PropertyValuesHolder[Integer.bitCount(start.mSetFlags)];
    int index = 0;

    if ((start.mSetFlags & TRANSLATION_X) != 0) {
      values[index++] = PropertyValuesHolder.ofFloat(
          View.TRANSLATION_X,
          start.mTranslationX,
          end.mTranslationX);
    }
    if ((start.mSetFlags & TRANSLATION_Y) != 0) {
      values[index++] = PropertyValuesHolder.ofFloat(
          View.TRANSLATION_Y,
          start.mTranslationY,
          end.mTranslationY);
    }
    if ((start.mSetFlags & SCALE_X) != 0) {
      values[index++] = PropertyValuesHolder.ofFloat(
          View.SCALE_X,
          start.mScaleX,
          end.mScaleX);
    }
    if ((start.mSetFlags & SCALE_Y) != 0) {
      values[index++] = PropertyValuesHolder.ofFloat(
          View.SCALE_Y,
          start.mScaleY,
          end.mScaleY);
    }
    if ((start.mSetFlags & ROTATION) != 0) {
      values[index++] = PropertyValuesHolder.ofFloat(
          View.ROTATION,
          start.mRotation,
          end.mRotation);
    }
    if ((start.mSetFlags & ROTATION_X) != 0) {
      values[index++] = PropertyValuesHolder.ofFloat(
          View.ROTATION_X,
          start.mRotationX,
          end.mRotationX);
    }
    if ((start.mSetFlags & ROTATION_Y) != 0) {
      values[index++] = PropertyValuesHolder.ofFloat(
          View.ROTATION_Y,
          start.mRotationY,
          end.mRotationY);
    }
    if ((start.mSetFlags & ALPHA) != 0) {
      values[index++] = PropertyValuesHolder.ofFloat(
          View.ALPHA,
          start.mAlpha,
          end.mAlpha);
    }

    return values;
  }
}
