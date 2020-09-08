/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.animation;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.litho.AnimatableItem;
import com.facebook.litho.LithoView;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RootHost;
import com.facebook.rendercore.transitions.TransitionRenderUnit;
import com.facebook.rendercore.transitions.TransitionUtils;
import com.facebook.rendercore.utils.BoundsUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** A convenience class for common View properties applicable to all subclasses of View. */
public final class AnimatedProperties {

  /**
   * The absolute X-position of a mount content, relative to the root {@link Host} that is rendering
   * this component tree.
   */
  public static final AnimatedProperty X = new XAnimatedProperty();

  /**
   * The absolute Y-position of a mount content, relative to the root {@link Host} that is rendering
   * this component tree.
   */
  public static final AnimatedProperty Y = new YAnimatedProperty();

  /** The width of a mount content. */
  public static final AnimatedProperty WIDTH = new WidthAnimatedProperty();

  /** The height of a mount content. */
  public static final AnimatedProperty HEIGHT = new HeightAnimatedProperty();

  /** The transparency of a mount content, where 0 is fully invisible and 1 is fully opaque. */
  public static final AnimatedProperty ALPHA = new AlphaAnimatedProperty();

  /**
   * The scale of a mount content: treats both X- and Y-scales as one.
   *
   * <p>The unit for scale is a percentage of the canonical dimensions of this content, with 1 =
   * original size (e.g. .5 = half the width/height, 2 = double the width/height).
   */
  public static final AnimatedProperty SCALE = new ScaleAnimatedProperty();

  /**
   * The width scale factor of a mount content.
   *
   * <p>The unit for scale is a percentage of the canonical width of this content, with 1 = original
   * width (e.g. .5 = half the width, 2 = double the width).
   */
  public static final AnimatedProperty SCALE_X = new ScaleXAnimatedProperty();

  /**
   * The height scale factor of a mount content.
   *
   * <p>The unit for scale is a percentage of the canonical height of this content, with 1 =
   * original height (e.g. .5 = half the height, 2 = double the height).
   */
  public static final AnimatedProperty SCALE_Y = new ScaleYAnimatedProperty();

  /**
   * The rotated degree around the pivot point of a mount content. Increasing the value results in
   * clockwise rotation.
   */
  public static final AnimatedProperty ROTATION = new RotationAnimatedProperty();

  public static final AnimatedProperty[] AUTO_LAYOUT_PROPERTIES =
      new AnimatedProperty[] {X, Y, WIDTH, HEIGHT};

  private AnimatedProperties() {}

  private static View assertIsView(Object mountContent, AnimatedProperty property) {
    if (!(mountContent instanceof View)) {
      throw new RuntimeException(
          "Animating '"
              + property.getName()
              + "' is only supported on Views (got "
              + mountContent
              + ")");
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
      if (mountContent instanceof Host && mountContent instanceof RootHost) {
        return ((Host) mountContent).getX();
      } else if (mountContent instanceof View) {
        return getPositionRelativeToRootHost((View) mountContent, true);
      } else if (mountContent instanceof Drawable) {
        final Drawable drawable = (Drawable) mountContent;
        float parentX = getPositionRelativeToRootHost(getHostView(drawable), true);
        return parentX + drawable.getBounds().left;
      } else {
        throw new UnsupportedOperationException(
            "Getting X from unsupported mount content: " + mountContent);
      }
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().left;
    }

    @Override
    public void set(Object mountContent, float value) {
      if (mountContent instanceof Host && mountContent instanceof RootHost) {
        ((View) mountContent).setX(value);
      } else if (mountContent instanceof View) {
        final View view = (View) mountContent;
        float parentX = getPositionRelativeToRootHost((View) view.getParent(), true);
        view.setX(value - parentX);
      } else if (mountContent instanceof Drawable) {
        final Drawable drawable = (Drawable) mountContent;
        float parentX = getPositionRelativeToRootHost(getHostView(drawable), true);
        TransitionUtils.applyXYToDrawableForAnimation(
            drawable, (int) (value - parentX), drawable.getBounds().top);
      } else {
        throw new UnsupportedOperationException(
            "Setting X on unsupported mount content: " + mountContent);
      }
    }

    @Override
    public void reset(Object mountContent) {
      if (mountContent instanceof View) {
        final View view = (View) mountContent;
        view.setTranslationX(0);
      } else if (mountContent instanceof Drawable) {
        // No-op: x/y are always properly set for Drawables
      }
    }
  }

  private static class YAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "y";
    }

    @Override
    public float get(Object mountContent) {
      if (mountContent instanceof Host && mountContent instanceof RootHost) {
        return ((Host) mountContent).getY();
      } else if (mountContent instanceof View) {
        return getPositionRelativeToRootHost((View) mountContent, false);
      } else if (mountContent instanceof Drawable) {
        final Drawable drawable = (Drawable) mountContent;
        float parentY = getPositionRelativeToRootHost(getHostView(drawable), false);
        return parentY + drawable.getBounds().top;
      } else {
        throw new UnsupportedOperationException(
            "Getting Y from unsupported mount content: " + mountContent);
      }
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().top;
    }

    @Override
    public void set(Object mountContent, float value) {
      if (mountContent instanceof Host && mountContent instanceof RootHost) {
        ((View) mountContent).setY(value);
      } else if (mountContent instanceof View) {
        final View view = (View) mountContent;
        float parentY = getPositionRelativeToRootHost((View) view.getParent(), false);
        view.setY(value - parentY);
      } else if (mountContent instanceof Drawable) {
        final Drawable drawable = (Drawable) mountContent;
        float parentY = getPositionRelativeToRootHost(getHostView(drawable), false);
        TransitionUtils.applyXYToDrawableForAnimation(
            drawable, drawable.getBounds().left, (int) (value - parentY));
      } else {
        throw new UnsupportedOperationException(
            "Setting Y on unsupported mount content: " + mountContent);
      }
    }

    @Override
    public void reset(Object mountContent) {
      if (mountContent instanceof View) {
        final View view = (View) mountContent;
        view.setTranslationY(0);
      } else if (mountContent instanceof Drawable) {
        // No-op: x/y are always properly set for Drawables
      }
    }
  };

  private static class WidthAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "width";
    }

    @Override
    public float get(Object mountContent) {
      if (mountContent instanceof View) {
        return ((View) mountContent).getWidth();
      } else if (mountContent instanceof Drawable) {
        return ((Drawable) mountContent).getBounds().width();
      } else {
        throw new UnsupportedOperationException(
            "Getting width from unsupported mount content: " + mountContent);
      }
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().width();
    }

    @Override
    public void set(Object mountContent, float value) {
      if (mountContent instanceof Host) {
        final Host view = (Host) mountContent;
        if (view instanceof LithoView) {
          ((LithoView) view).setAnimatedWidth((int) value);
        } else {
          final int left = view.getLeft();
          BoundsUtils.applyBoundsToMountContent(
              left, view.getTop(), (int) (left + value), view.getBottom(), null, view, false);
        }

        final List<Drawable> animatingDrawables = getLinkedDrawables(view);
        if (animatingDrawables != null) {
          final int width = (int) value;
          final int height = view.getHeight();
          for (int index = 0; index < animatingDrawables.size(); ++index) {
            TransitionUtils.applySizeToDrawableForAnimation(
                animatingDrawables.get(index), width, height);
          }
        }
      } else if (mountContent instanceof View) {
        final View view = (View) mountContent;
        final int left = view.getLeft();
        final int right = (int) (left + value);
        BoundsUtils.applyBoundsToMountContent(
            left, view.getTop(), right, view.getBottom(), null, view, false);
      } else if (mountContent instanceof Drawable) {
        final Drawable drawable = (Drawable) mountContent;
        final int width = (int) value;
        final int height = drawable.getBounds().height();
        TransitionUtils.applySizeToDrawableForAnimation(drawable, width, height);
      } else {
        throw new UnsupportedOperationException(
            "Setting width on unsupported mount content: " + mountContent);
      }
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
      if (mountContent instanceof View) {
        return ((View) mountContent).getHeight();
      } else if (mountContent instanceof Drawable) {
        return ((Drawable) mountContent).getBounds().height();
      } else {
        throw new UnsupportedOperationException(
            "Getting height from unsupported mount content: " + mountContent);
      }
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.getBounds().height();
    }

    @Override
    public void set(Object mountContent, float value) {
      if (mountContent instanceof Host) {
        final Host view = (Host) mountContent;
        if (view instanceof LithoView) {
          ((LithoView) view).setAnimatedHeight((int) value);
        } else {
          final int top = view.getTop();
          BoundsUtils.applyBoundsToMountContent(
              view.getLeft(), top, view.getRight(), (int) (top + value), null, view, false);
        }

        final List<Drawable> animatingDrawables = getLinkedDrawables(view);
        if (animatingDrawables != null) {
          final int width = view.getWidth();
          final int height = (int) value;
          for (int index = 0; index < animatingDrawables.size(); ++index) {
            TransitionUtils.applySizeToDrawableForAnimation(
                animatingDrawables.get(index), width, height);
          }
        }
      } else if (mountContent instanceof View) {
        final View view = (View) mountContent;
        final int top = view.getTop();
        final int bottom = (int) (top + value);
        BoundsUtils.applyBoundsToMountContent(
            view.getLeft(), top, view.getRight(), bottom, null, view, false);
      } else if (mountContent instanceof Drawable) {
        final Drawable drawable = (Drawable) mountContent;
        final int width = drawable.getBounds().width();
        final int height = (int) value;
        TransitionUtils.applySizeToDrawableForAnimation(drawable, width, height);
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
      if (mountContent instanceof View) {
        return ((View) mountContent).getAlpha();
      } else {
        throw new UnsupportedOperationException(
            "Tried to get alpha of unsupported mount content: " + mountContent);
      }
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.isAlphaSet() ? animatableItem.getAlpha() : 1;
    }

    @Override
    public void set(Object mountContent, float value) {
      if (mountContent instanceof View) {
        ((View) mountContent).setAlpha(value);
      } else {
        throw new UnsupportedOperationException(
            "Setting alpha on unsupported mount content: " + mountContent);
      }
    }

    @Override
    public void reset(Object mountContent) {
      set(mountContent, 1);
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
      final View asView = assertIsView(mountContent, this);
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

  private static class RotationAnimatedProperty implements AnimatedProperty {
    @Override
    public String getName() {
      return "rotation";
    }

    @Override
    public float get(Object mountContent) {
      return assertIsView(mountContent, this).getRotation();
    }

    @Override
    public float get(AnimatableItem animatableItem) {
      return animatableItem.isRotationSet() ? animatableItem.getRotation() : 0;
    }

    @Override
    public void set(Object mountContent, float value) {
      assertIsView(mountContent, this).setRotation(value);
    }

    @Override
    public void reset(Object mountContent) {
      assertIsView(mountContent, this).setRotation(0);
    }
  }

  /**
   * @return the x or y position of the given view relative to the root {@link Host} that this
   *     ComponentTree is being rendered in to.
   */
  private static float getPositionRelativeToRootHost(View mountContent, boolean getX) {
    float pos = 0;
    @Nullable View currentView = mountContent;
    while (true) {
      if (currentView == null || !(currentView.getParent() instanceof View)) {
        return pos;
      }
      if (currentView instanceof Host && currentView instanceof RootHost) {
        return pos;
      }
      pos += getX ? currentView.getX() : currentView.getY();
      currentView = (View) currentView.getParent();
    }
  }

  @Nullable
  private static View getHostView(Drawable drawable) {
    Drawable.Callback callback;
    while (true) {
      callback = drawable.getCallback();
      if (callback instanceof Drawable) {
        drawable = (Drawable) callback;
      } else if (callback instanceof View) {
        return (View) callback;
      } else {
        return null;
      }
    }
  }

  private static @Nullable List<Drawable> getLinkedDrawables(Host host) {
    List<Drawable> drawables = null;

    for (int i = 0, size = host.getMountItemCount(); i < size; i++) {
      final MountItem mountItem = host.getMountItemAt(i);
      if (mountItem.getContent() instanceof Drawable
          && mountItem.getRenderTreeNode().getRenderUnit() instanceof TransitionRenderUnit
          && ((TransitionRenderUnit) mountItem.getRenderTreeNode().getRenderUnit())
              .getMatchHostBounds()) {
        if (drawables == null) {
          drawables = new ArrayList<>();
        }
        drawables.add((Drawable) mountItem.getContent());
      }
    }
    return drawables;
  }
}
