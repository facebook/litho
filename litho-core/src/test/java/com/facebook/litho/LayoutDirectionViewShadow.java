/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

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
