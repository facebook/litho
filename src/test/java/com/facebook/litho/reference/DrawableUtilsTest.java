/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class DrawableUtilsTest {

  @Test
  public void layerDrawableEqualityTest() {
    LayerDrawable layerDrawable = getTestLayerDrawable();
    LayerDrawable secondDrawable = getTestLayerDrawable();
    LayerDrawable differentDrawable = getDifferentTestLayerDrawable();

    assertTrue(DrawableUtils.areDrawablesEqual(layerDrawable, secondDrawable));
    assertFalse(DrawableUtils.areDrawablesEqual(layerDrawable, differentDrawable));
  }

  @Test
  public void colorDrawableEqualityTest() {
    ColorDrawable drawable = new ColorDrawable(Color.BLACK);
    ColorDrawable sameDrawable = new ColorDrawable(Color.BLACK);
    ColorDrawable differentDrawable = new ColorDrawable(Color.BLACK);
    differentDrawable.setAlpha(127);

    assertTrue(DrawableUtils.areDrawablesEqual(drawable, sameDrawable));
    assertFalse(DrawableUtils.areDrawablesEqual(drawable, differentDrawable));
  }

  @Test
  public void nullDrawableEqualityTest() {
    ColorDrawable drawable = new ColorDrawable(Color.BLACK);
    assertFalse(DrawableUtils.areDrawablesEqual(drawable, null));
  }

  @Test
  public void layerDrawableInsetsTest() {
    LayerDrawable layerDrawable = getTestLayerDrawable();
    LayerDrawable secondDrawable = getTestLayerDrawable();

    layerDrawable.setLayerInset(0, 5, 5, 5, 5);
    layerDrawable.setLayerInset(1, 5, 5, 5, 5);
    layerDrawable.setLayerInset(2, 5, 5, 5, 5);

    secondDrawable.setLayerInset(0, 5, 5, 5, 5);
    secondDrawable.setLayerInset(1, 5, 5, 5, 5);
    secondDrawable.setLayerInset(2, 5, 5, 5, 5);

    assertTrue(DrawableUtils.areDrawablesEqual(layerDrawable, secondDrawable));

    secondDrawable.setLayerInset(1, 0, 5, 5, 5);

    assertFalse(DrawableUtils.areDrawablesEqual(layerDrawable, secondDrawable));
  }

  private static LayerDrawable getTestLayerDrawable() {
    Drawable[] layers = new Drawable[3];
    layers[0] = new ColorDrawable(Color.BLACK);
    layers[1] = new ColorDrawable(Color.CYAN);
    layers[2] = new ColorDrawable(Color.CYAN);

    return new LayerDrawable(layers);
  }

  private static LayerDrawable getDifferentTestLayerDrawable() {
    Drawable[] layers = new Drawable[3];
    layers[0] = new ColorDrawable(Color.BLACK);
    layers[1] = new ColorDrawable(Color.CYAN);
    layers[2] = new ColorDrawable(Color.BLACK);

    return new LayerDrawable(layers);
  }
}
