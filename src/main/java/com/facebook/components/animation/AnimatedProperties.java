// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

import android.view.View;

/**
 * A convenience class for common View properties applicable to all subclasses of View.
 */
public final class AnimatedProperties {

  /**
   * The X-position of a mount item, relative to its parent.
   */
  public static final AnimatedProperty X = new XAnimatedProperty();

  /**
   * The Y-position of a mount item, relative to its parent.
   */
  public static final AnimatedProperty Y = new YAnimatedProperty();

  /**
   * The transparency of a mount item.
   */
  public static final AnimatedProperty ALPHA = new AlphaAnimatedProperty();

  /**
   * The scale of a mount item: treats both X- and Y-scales as one.
   */
  public static final AnimatedProperty SCALE = new ScaleAnimatedProperty();

  private AnimatedProperties() {
  }

  private static View assertIsView(Object mountItem, AnimatedProperty property) {
    if (!(mountItem instanceof View)) {
      throw new RuntimeException(
          "Animating '" + property.getName() + "' is only supported on Views");
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
      return assertIsView(mountItem, this).getX();
    }

    @Override
    public void set(Object mountItem, float value) {
      assertIsView(mountItem, this).setX(value);
    }
  }

  private static class YAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "y";
    }

    @Override
    public float get(Object mountItem) {
      return assertIsView(mountItem, this).getY();
    }

    @Override
    public void set(Object mountItem, float value) {
      assertIsView(mountItem, this).setY(value);
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
    public void set(Object mountItem, float value) {
      assertIsView(mountItem, this).setAlpha(value);
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
    public void set(Object mountItem, float value) {
      final View asView = assertIsView(mountItem, this);
      asView.setScaleX(value);
      asView.setScaleY(value);
    }
  }
}
