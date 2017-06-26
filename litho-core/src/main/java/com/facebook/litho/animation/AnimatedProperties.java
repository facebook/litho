/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import android.view.View;

import com.facebook.litho.AnimatableItem;
import com.facebook.litho.LithoView;

/**
 * A convenience class for common View properties applicable to all subclasses of View.
 */
public final class AnimatedProperties {

  /**
   * The absolute X-position of a mount item, relative to the {@link com.facebook.litho.LithoView}
   * that is rendering this component tree.
   */
  public static final AnimatedProperty X = new XAnimatedProperty();

  /**
   * The absolute Y-position of a mount item, relative to the {@link com.facebook.litho.LithoView}
   * that is rendering this component tree.
   */
  public static final AnimatedProperty Y = new YAnimatedProperty();

  /**
   * The width of a mount item.
   */
  public static final AnimatedProperty WIDTH = new WidthAnimatedProperty();

  /**
   * The height of a mount item.
   */
  public static final AnimatedProperty HEIGHT = new HeightAnimatedProperty();

  /**
   * The transparency of a mount item.
   */
  public static final AnimatedProperty ALPHA = new AlphaAnimatedProperty();

  /**
   * The scale of a mount item: treats both X- and Y-scales as one.
   */
  public static final AnimatedProperty SCALE = new ScaleAnimatedProperty();

  /**
   * The x-scale of a mount item.
   */
  public static final AnimatedProperty SCALE_X = new ScaleXAnimatedProperty();

  /**
   * The y-scale of a mount item.
   */
  public static final AnimatedProperty SCALE_Y = new ScaleYAnimatedProperty();

  private AnimatedProperties() {
  }

  private static View assertIsView(Object mountItem, AnimatedProperty property) {
    if (!(mountItem instanceof View)) {
      throw new RuntimeException(
          "Animating '" + property.getName() + "' is only supported on Views (got " + mountItem +
              ")");
    }
    return (View) mountItem;
  }

  private static class XAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "x";
    }

    @Override
    public float get(Object mountItem) {
      return getPositionRelativeToLithoView(assertIsView(mountItem, this), true);
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().left;
    }

    @Override
    public void set(Object mountItem, float value) {
      View mountView = assertIsView(mountItem, this);
      float parentX = getPositionRelativeToLithoView((View) mountView.getParent(), true);
      mountView.setX(value - parentX);
    }

    @Override
    public void reset(Object mountItem) {
      assertIsView(mountItem, this).setTranslationX(0);
    }
  }

  private static class YAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "y";
    }

    @Override
    public float get(Object mountItem) {
      return getPositionRelativeToLithoView(assertIsView(mountItem, this), false);
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().top;
    }

    @Override
    public void set(Object mountItem, float value) {
      View mountView = assertIsView(mountItem, this);
      float parentY = getPositionRelativeToLithoView((View) mountView.getParent(), false);
      mountView.setY(value - parentY);
    }

    @Override
    public void reset(Object mountItem) {
      assertIsView(mountItem, this).setTranslationY(0);
    }
  };

  private static class WidthAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "width";
    }

    @Override
    public float get(Object mountItem) {
      return assertIsView(mountItem, this).getWidth();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().width();
    }

    @Override
    public void set(Object mountItem, float value) {
      throw new UnsupportedOperationException("Setting width in animations is not supported yet.");
    }

    @Override
    public void reset(Object mountItem) {
      // No-op: height/width are always properly set at mount time so we don't need to reset it.
    }
  }

  private static class HeightAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "height";
    }

    @Override
    public float get(Object mountItem) {
      return assertIsView(mountItem, this).getHeight();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().height();
    }

    @Override
    public void set(Object mountItem, float value) {
      throw new UnsupportedOperationException("Setting height in animations is not supported yet.");
    }

    @Override
    public void reset(Object mountItem) {
      // No-op: height/width are always properly set at mount time so we don't need to reset it.
    }
  };

  private static class AlphaAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "alpha";
    }

    @Override
    public float get(Object mountItem) {
      return assertIsView(mountItem, this).getAlpha();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return 1;
    }

    @Override
    public void set(Object mountItem, float value) {
      assertIsView(mountItem, this).setAlpha(value);
    }

    @Override
    public void reset(Object mountItem) {
      assertIsView(mountItem, this).setAlpha(1);
    }
  }

  private static class ScaleAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "scale";
    }

    @Override
    public float get(Object mountItem) {
      final View asView = assertIsView(mountItem, this);
      final float scale = asView.getScaleX();
      if (scale != asView.getScaleY()) {
        throw new RuntimeException(
            "Tried to get scale of view, but scaleX and scaleY are different");
      }
      return scale;
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return 1;
    }

    @Override
    public void set(Object mountItem, float value) {
      final View asView = assertIsView(mountItem, this);
      asView.setScaleX(value);
      asView.setScaleY(value);
    }

    @Override
    public void reset(Object mountItem) {
      final View asView = assertIsView(mountItem, this);
      asView.setScaleX(1);
      asView.setScaleY(1);
    }
  }

  private static class ScaleXAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "scale_x";
    }

    @Override
    public float get(Object mountItem) {
      return assertIsView(mountItem, this).getScaleX();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return 1;
    }

    @Override
    public void set(Object mountItem, float value) {
      assertIsView(mountItem, this).setScaleX(value);
    }

    @Override
    public void reset(Object mountItem) {
      assertIsView(mountItem, this).setScaleX(1);
    }
  }

  private static class ScaleYAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "scale_y";
    }

    @Override
    public float get(Object mountItem) {
      return assertIsView(mountItem, this).getScaleY();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return 1;
    }

    @Override
    public void set(Object mountItem, float value) {
      assertIsView(mountItem, this).setScaleY(value);
    }

    @Override
    public void reset(Object mountItem) {
      assertIsView(mountItem, this).setScaleY(1);
    }
  }

  /**
   * @return the x or y position of the given view relative to the LithoView that this ComponentTree
   * is being rendered in to.
   */
  private static float getPositionRelativeToLithoView(View mountItem, boolean getX) {
    float pos = 0;
    View currentView = mountItem;
    while (true) {
      if (currentView == null) {
        throw new RuntimeException("Got unexpected null parent");
      }
      if (currentView instanceof LithoView) {
        return pos;
      }
      pos += getX ? currentView.getX() : currentView.getY();
      if (!(mountItem.getParent() instanceof View)) {
        throw new RuntimeException("Expected parent to be View, was " + mountItem.getParent());
      }
      currentView = (View) currentView.getParent();
    }
  }
}
