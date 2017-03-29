/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.litho.config.ComponentsConfiguration;

import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.Component.isMountViewSpec;

/**
 * Draw operations used in developer options.
 */
class DebugDraw {

  private static final int INTERACTIVE_VIEW_COLOR = 0x66C29BFF;
  private static final int TOUCH_DELEGATE_COLOR = 0x44D3FFCE;

  private static final int MOUNT_BORDER_COLOR = 0x99FF0000;
  private static final int MOUNT_BORDER_COLOR_HOST = 0x99FF00FF;
  private static final int MOUNT_CORNER_COLOR = 0xFF0000FF;
  private static final int MOUNT_CORNER_COLOR_HOST = 0xFF00FFFF;

  private static Paint sInteractiveViewPaint;
  private static Paint sTouchDelegatePaint;

  private static Rect sMountBoundsRect;
  private static Paint sMountBoundsBorderPaint;
  private static Paint sMountBoundsCornerPaint;

  static void draw(ComponentHost host, Canvas canvas) {
    if (ComponentsConfiguration.debugHighlightInteractiveBounds) {
      highlightInteractiveBounds(host, canvas);
    }

    if (ComponentsConfiguration.debugHighlightMountBounds) {
      highlightMountBounds(host, canvas);
    }
  }

  private static void highlightInteractiveBounds(ComponentHost host, Canvas canvas) {
    if (sInteractiveViewPaint == null) {
      sInteractiveViewPaint = new Paint();
      sInteractiveViewPaint.setColor(INTERACTIVE_VIEW_COLOR);
    }

    if (sTouchDelegatePaint == null) {
      sTouchDelegatePaint = new Paint();
      sTouchDelegatePaint.setColor(TOUCH_DELEGATE_COLOR);
    }

    // 1. Highlight root, if applicable.
    if (isInteractive(host)) {
      canvas.drawRect(0, 0, host.getWidth(), host.getHeight(), sInteractiveViewPaint);
    }

    // 2. Highlight non-host interactive mounted views.
    for (int i = host.getMountItemCount() - 1; i >= 0; i--) {
      final MountItem item = host.getMountItemAt(i);

      final Component<?> component = item.getComponent();
      if (!isMountViewSpec(component) || isHostSpec(component)) {
        continue;
      }

      final View view = (View) item.getContent();
      if (!isInteractive(view)) {
        continue;
      }

      canvas.drawRect(
          view.getLeft(),
          view.getTop(),
          view.getRight(),
          view.getBottom(),
          sTouchDelegatePaint);
    }

    // 3. Highlight expanded touch bounds.
    final TouchExpansionDelegate touchDelegate = host.getTouchExpansionDelegate();
    if (touchDelegate != null) {
      touchDelegate.draw(canvas, sTouchDelegatePaint);
    }
  }

  private static void highlightMountBounds(ComponentHost host, Canvas canvas) {
    final Resources resources = host.getResources();

    if (sMountBoundsRect == null) {
      sMountBoundsRect = new Rect();
    }

    if (sMountBoundsBorderPaint == null) {
      sMountBoundsBorderPaint = new Paint();
      sMountBoundsBorderPaint.setStyle(Style.STROKE);
      sMountBoundsBorderPaint.setStrokeWidth(dipToPixels(resources, 1));
    }

    if (sMountBoundsCornerPaint == null) {
      sMountBoundsCornerPaint = new Paint();
      sMountBoundsCornerPaint.setStyle(Style.FILL);
      sMountBoundsCornerPaint.setStrokeWidth(dipToPixels(resources, 2));
    }

    for (int i = host.getMountItemCount() - 1; i >= 0; i--) {
      final MountItem item = host.getMountItemAt(i);

      final Component<?> component = item.getComponent();
      final Object content = item.getContent();

      if (!shouldHighlight(component)) {
        continue;
      }

      if (content instanceof View) {
        final View view = (View) content;
        sMountBoundsRect.left = view.getLeft();
        sMountBoundsRect.top = view.getTop();
        sMountBoundsRect.right = view.getRight();
        sMountBoundsRect.bottom = view.getBottom();
      } else if (content instanceof Drawable) {
        final Drawable drawable = (Drawable) content;
        sMountBoundsRect.set(drawable.getBounds());
      }

      sMountBoundsBorderPaint.setColor(getBorderColor(component));
      drawMountBoundsBorder(canvas, sMountBoundsBorderPaint, sMountBoundsRect);

      sMountBoundsCornerPaint.setColor(getCornerColor(component));
      drawMountBoundsCorners(
          canvas,
          sMountBoundsCornerPaint,
          sMountBoundsRect,
          (int) sMountBoundsCornerPaint.getStrokeWidth(),
          Math.min(
              Math.min(sMountBoundsRect.width(), sMountBoundsRect.height()) / 3,
              dipToPixels(resources, 12)));
    }
  }

  private static void drawMountBoundsBorder(Canvas canvas, Paint paint, Rect bounds) {
    final int inset = (int) paint.getStrokeWidth() / 2;
    canvas.drawRect(
        bounds.left + inset,
        bounds.top + inset,
        bounds.right - inset,
        bounds.bottom - inset,
        paint);
  }

  private static void drawMountBoundsCorners(
      Canvas canvas,
      Paint paint,
      Rect bounds,
      int cornerLength,
      int cornerWidth) {

    drawCorner(
        canvas,
        paint,
        bounds.left,
        bounds.top,
        cornerLength,
        cornerLength,
        cornerWidth);

    drawCorner(
        canvas,
        paint,
        bounds.left,
        bounds.bottom,
        cornerLength,
        -cornerLength,
        cornerWidth);

    drawCorner(
        canvas,
        paint,
        bounds.right,
        bounds.top,
        -cornerLength,
        cornerLength,
        cornerWidth);

    drawCorner(
        canvas,
        paint,
        bounds.right,
        bounds.bottom,
        -cornerLength,
        -cornerLength,
        cornerWidth);
  }

  private static boolean shouldHighlight(Component<?> component) {
    // Don't highlight bounds of background/foreground components.
    return !(component.getLifecycle() instanceof DrawableComponent);
  }

  private static int dipToPixels(Resources res, int dips) {
    float scale = res.getDisplayMetrics().density;
    return (int) (dips * scale + 0.5f);
  }

  private static int getBorderColor(Component<?> component) {
    return isHostSpec(component) ? MOUNT_BORDER_COLOR_HOST : MOUNT_BORDER_COLOR;
  }

  private static int getCornerColor(Component<?> component) {
    return isHostSpec(component) ? MOUNT_CORNER_COLOR_HOST : MOUNT_CORNER_COLOR;
  }

  private static int sign(float x) {
    return (x >= 0) ? 1 : -1;
  }

  private static void drawCorner(
      Canvas c,
      Paint paint,
      int x,
      int y,
      int dx,
      int dy,
      int cornerWidth) {
    drawCornerLine(c, paint, x, y, x + dx, y + cornerWidth * sign(dy));
    drawCornerLine(c, paint, x, y, x + cornerWidth * sign(dx), y + dy);
  }

  private static void drawCornerLine(
      Canvas canvas,
      Paint paint,
      int left,
      int top,
      int right,
      int bottom) {

    if (left > right) {
      final int tmp = left;
      left = right;
      right = tmp;
    }

    if (top > bottom) {
      final int tmp = top;
      top = bottom;
      bottom = tmp;
    }

    canvas.drawRect(left, top, right, bottom, paint);
  }

  private static boolean isInteractive(View view) {
    return MountState.getComponentClickListener(view) != null
        || MountState.getComponentLongClickListener(view) != null
        || MountState.getComponentTouchListener(view) != null;
  }
}
