// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.view.View;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowView;

/**
 * Robolectric shadow view does not support layout direction so we must implement our custom shadow.
 * We must have ViewGroup and View shadows as Robolectric forces us to have the whole hierarchy.
 */
@Implements(View.class)
public class LayoutDirectionViewShadow extends ShadowView {
  private int mLayoutDirection = View.LAYOUT_DIRECTION_LTR;

  @Implementation
  public int getLayoutDirection() {
    return mLayoutDirection;
  }

  @Implementation
  public void setLayoutDirection(int layoutDirection) {
    mLayoutDirection = layoutDirection;
  }
}
