/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.litho.AnimatableItem;
import com.facebook.litho.BoundsHelper;
import com.facebook.litho.ComponentHost;
import com.facebook.litho.LithoView;

/**
 * A convenience class for common View properties applicable to all subclasses of View.
 */
public final class AnimatedProperties {

  /**
   * The absolute X-position of a mount content, relative to the
   * {@link com.facebook.litho.LithoView} that is rendering this component tree.
   */
  public static final AnimatedProperty X = new XAnimatedProperty();

  /**
   * The absolute Y-position of a mount content, relative to the
   * {@link com.facebook.litho.LithoView} that is rendering this component tree.
   */
  public static final AnimatedProperty Y = new YAnimatedProperty();

  /**
   * The width of a mount content.
   */
  public static final AnimatedProperty WIDTH = new WidthAnimatedProperty();

  /**
   * The height of a mount content.
   */
  public static final AnimatedProperty HEIGHT = new HeightAnimatedProperty();

  /**
   * The transparency of a mount content.
   */
  public static final AnimatedProperty ALPHA = new AlphaAnimatedProperty();

  /**
   * The scale of a mount content: treats both X- and Y-scales as one.
   */
  public static final AnimatedProperty SCALE = new ScaleAnimatedProperty();

  /**
   * The x-scale of a mount content.
   */
  public static final AnimatedProperty SCALE_X = new ScaleXAnimatedProperty();

  /**
   * The y-scale of a mount content.
   */
  public static final AnimatedProperty SCALE_Y = new ScaleYAnimatedProperty();

  public static final AnimatedProperty[] ALL_PROPERTIES = new AnimatedProperty[] {
      X,
      Y,
      ALPHA
  };

  private AnimatedProperties() {
  }

  private static View assertIsView(Object mountContent, AnimatedProperty property) {
    if (!(mountContent instanceof View)) {
      throw new RuntimeException(
          "Animating '" + property.getName() + "' is only supported on Views (got " + mountContent +
              ")");
    }
    return (View) mountContent;
  }

  private static class XAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "x";
    }

    @Override
    public float get(Object mountContent) {
      return getPositionRelativeToLithoView(assertIsView(mountContent, this), true);
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().left;
    }

    @Override
    public void set(Object mountContent, float value) {
      View mountView = assertIsView(mountContent, this);
      float parentX = getPositionRelativeToLithoView((View) mountView.getParent(), true);
      mountView.setX(value - parentX);
    }

    @Override
    public void reset(Object mountContent) {
      assertIsView(mountContent, this).setTranslationX(0);
    }
  }

  private static class YAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "y";
    }

    @Override
    public float get(Object mountContent) {
      return getPositionRelativeToLithoView(assertIsView(mountContent, this), false);
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().top;
    }

    @Override
    public void set(Object mountContent, float value) {
      View mountView = assertIsView(mountContent, this);
      float parentY = getPositionRelativeToLithoView((View) mountView.getParent(), false);
      mountView.setY(value - parentY);
    }

    @Override
    public void reset(Object mountContent) {
      assertIsView(mountContent, this).setTranslationY(0);
    }
  };

  private static class WidthAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "width";
    }

    @Override
    public float get(Object mountContent) {
      return assertIsView(mountContent, this).getWidth();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().width();
    }

    @Override
    public void set(Object mountContent, float value) {
      throw new UnsupportedOperationException("Setting width in animations is not supported yet.");
    }

    @Override
    public void reset(Object mountContent) {
      // No-op: height/width are always properly set at mount time so we don't need to reset it.
    }
  }

  private static class HeightAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "height";
    }

    @Override
    public float get(Object mountContent) {
      return assertIsView(mountContent, this).getHeight();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().height();
    }

    @Override
    public void set(Object mountContent, float value) {
      if (mountContent instanceof ComponentHost) {
        final ComponentHost view = (ComponentHost) mountContent;
        if (view instanceof LithoView && ((LithoView) view).isExpectingBoundsAnimation()) {
          ((LithoView) view).setAnimatedHeight((int) value);
        } else {
          final int top = view.getTop();
          BoundsHelper.applyBoundsToView(
              view, view.getLeft(), top, view.getRight(), (int) (top + value), false);
        }

        final Drawable animatingMountItem = view.getLinkedDrawableForAnimation();
        if (animatingMountItem != null) {
          BoundsHelper.applySizeToDrawableForAnimation(
              animatingMountItem, view.getWidth(), (int) (value));
        }
      } else {
        throw new UnsupportedOperationException(
            "Setting height on unsupported mount content: " + mountContent);
      }
    }

    @Override
    public void reset(Object mountContent) {
      // No-op: height/width are always properly set at mount time so we don't need to reset it.
    }
  }

  private static class AlphaAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "alpha";
    }

    @Override
    public float get(Object mountContent) {
      return assertIsView(mountContent, this).getAlpha();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.isAlphaSet() ? animatableItem.getAlpha() : 1;
    }

    @Override
    public void set(Object mountContent, float value) {
      assertIsView(mountContent, this).setAlpha(value);
    }

    @Override
    public void reset(Object mountContent) {
      // TODO(t22367997): Reset alpha based on final LayoutOutput instead of always resetting to 1
      assertIsView(mountContent, this).setAlpha(1);
    }
  }

  private static class ScaleAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "scale";
    }

    @Override
    public float get(Object mountContent) {
      final View asView = assertIsView(mountContent, this);
      final float scale = asView.getScaleX();
      if (scale != asView.getScaleY()) {
        throw new RuntimeException(
            "Tried to get scale of view, but scaleX and scaleY are different");
      }
      return scale;
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.isScaleSet() ? animatableItem.getScale() : 1;
    }

    @Override
    public void set(Object mountContent, float value) {
      final View asView = assertIsView(mountContent, this);
      asView.setScaleX(value);
      asView.setScaleY(value);
    }

    @Override
    public void reset(Object mountContent) {
      // This gets reset in MountState.
    }
  }

  private static class ScaleXAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "scale_x";
    }

    @Override
    public float get(Object mountContent) {
      return assertIsView(mountContent, this).getScaleX();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return 1;
    }

    @Override
    public void set(Object mountContent, float value) {
      assertIsView(mountContent, this).setScaleX(value);
    }

    @Override
    public void reset(Object mountContent) {
      assertIsView(mountContent, this).setScaleX(1);
    }
  }

  private static class ScaleYAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "scale_y";
    }

    @Override
    public float get(Object mountContent) {
      return assertIsView(mountContent, this).getScaleY();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return 1;
    }

    @Override
    public void set(Object mountContent, float value) {
      assertIsView(mountContent, this).setScaleY(value);
    }

    @Override
    public void reset(Object mountContent) {
      assertIsView(mountContent, this).setScaleY(1);
    }
  }

  /**
   * @return the x or y position of the given view relative to the LithoView that this ComponentTree
   * is being rendered in to.
   */
  private static float getPositionRelativeToLithoView(View mountContent, boolean getX) {
    float pos = 0;
    View currentView = mountContent;
    while (true) {
      if (currentView == null) {
        throw new RuntimeException("Got unexpected null parent");
      }
      if (currentView instanceof LithoView) {
        return pos;
      }
      pos += getX ? currentView.getX() : currentView.getY();
      if (!(mountContent.getParent() instanceof View)) {
        throw new RuntimeException("Expected parent to be View, was " + mountContent.getParent());
      }
      currentView = (View) currentView.getParent();
    }
  }
}
