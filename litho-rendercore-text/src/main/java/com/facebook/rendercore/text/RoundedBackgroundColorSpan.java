// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore.text;

import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.Layout;
import android.text.TextUtils;
import android.text.style.LineBackgroundSpan;
import androidx.annotation.ColorInt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Draws a rounded background behind text. */
public class RoundedBackgroundColorSpan implements LineBackgroundSpan {
  private final Paint mPaint;
  private final List<Path> mPaths;

  public RoundedBackgroundColorSpan(
      final Layout layout,
      @ColorInt final int color,
      final float startPadding,
      final float endPadding,
      final float topPadding,
      final float bottomPadding,
      final float cornerRadius) {
    mPaths = getPaths(layout, startPadding, endPadding, topPadding, bottomPadding, cornerRadius);
    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPaint.setColor(color);
    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    mPaint.setPathEffect(new CornerPathEffect(cornerRadius));
  }

  @Override
  public void drawBackground(
      Canvas canvas,
      Paint paint,
      int left,
      int right,
      int top,
      int baseline,
      int bottom,
      CharSequence text,
      int start,
      int end,
      int lineNumber) {
    for (Path path : mPaths) {
      canvas.drawPath(path, mPaint);
    }
  }

  private static List<Path> getPaths(
      Layout layout,
      float startPadding,
      float endPadding,
      float topPadding,
      float bottomPadding,
      float cornerRadius) {
    // Build up a list of continuous line boundaries as a list of lists of rects. Each sublist
    // is a continuous block of text. If an empty line is encountered we start building a new list
    // of rects from the next non-empty line.
    List<List<RectF>> shapes = new ArrayList<>();
    List<RectF> lineBounds = new ArrayList<>();

    for (int line = 0; line < layout.getLineCount(); line++) {
      float lineLeft = layout.getLineLeft(line);
      float lineTop = layout.getLineTop(line);
      float lineRight = layout.getLineRight(line);
      float lineBottom = layout.getLineBottom(line);
      RectF lineRect = new RectF(lineLeft, lineTop, lineRight, lineBottom);

      int lineStart = layout.getLineStart(line);
      int lineEnd = layout.getLineEnd(line);
      String lineText = layout.getText().subSequence(lineStart, lineEnd).toString();

      if (lineRect.width() > 0 && !TextUtils.isEmpty(lineText.replace("\n", ""))) {
        lineBounds.add(lineRect);
      } else if (!lineBounds.isEmpty()) {
        shapes.add(lineBounds);
        lineBounds = new ArrayList<>();
      }
    }

    if (!lineBounds.isEmpty()) {
      shapes.add(lineBounds);
    }

    // Transform the list of shapes to paths
    List<Path> paths = new ArrayList<>();

    int lineCount;

    for (int i = 0; i < shapes.size(); i++) {
      lineBounds = shapes.get(i);

      Path path = new Path();
      paths.add(path);

      lineCount = lineBounds.size();
      PointF[] leftSide = new PointF[lineCount * 2];
      PointF[] rightSide = new PointF[lineCount * 2];

      // Build up arrays of Points for representing the left and right side of the final path.
      for (int j = 0; j <= lineCount - 1; j++) {
        RectF bounds = lineBounds.get(j);
        int topPointIdx = j * 2;
        int bottomPointIdx = topPointIdx + 1;
        rightSide[topPointIdx] = new PointF(bounds.right + endPadding, bounds.top - topPadding);
        rightSide[bottomPointIdx] =
            new PointF(bounds.right + endPadding, bounds.bottom + bottomPadding);
        leftSide[topPointIdx] = new PointF(bounds.left - startPadding, bounds.top - topPadding);
        leftSide[bottomPointIdx] =
            new PointF(bounds.left - startPadding, bounds.bottom + bottomPadding);
      }

      ensureNinetyDegreeAngles(rightSide, true);
      ensureNinetyDegreeAngles(leftSide, false);

      // Adjust the right and left point arrays to ensure minimum horizontal offsets between
      // vertical edges.
      List<PointF> rightSideAdjustedPoints =
          ensureMinimumHorizontalDistanceBetweenVerticalEdgesForSide(
              rightSide, cornerRadius, true /* right side */);
      List<PointF> leftSideAdjustedPoints =
          ensureMinimumHorizontalDistanceBetweenVerticalEdgesForSide(
              leftSide, cornerRadius, false /* left side */);

      // Iterate the point arrays to build the final path.
      path.moveTo(rightSideAdjustedPoints.get(0).x, rightSideAdjustedPoints.get(0).y);
      for (int j = 1; j < rightSideAdjustedPoints.size(); j++) {
        path.lineTo(rightSideAdjustedPoints.get(j).x, rightSideAdjustedPoints.get(j).y);
      }
      for (int j = leftSideAdjustedPoints.size() - 1; j >= 0; j--) {
        path.lineTo(leftSideAdjustedPoints.get(j).x, leftSideAdjustedPoints.get(j).y);
      }

      path.close();
    }
    return paths;
  }

  /**
   * Ensure that horizontal edges are all 90 degrees to the vertical edges.
   *
   * @param points the points for a side
   * @param rightSide true if this is the right side, false if it is the left
   */
  private static void ensureNinetyDegreeAngles(PointF[] points, boolean rightSide) {
    for (int i = 1; i < points.length; i++) {
      PointF point = points[i];
      PointF priorPoint = points[i - 1];

      if (rightSide) {
        if (point.x > priorPoint.x) {
          // Current point is the outer corner so raise the prior point to match.
          priorPoint.y = point.y;
        } else if (point.x < priorPoint.x) {
          // Prior point is the outer corner so lower the current point to match.
          point.y = priorPoint.y;
        }
      } else {
        if (point.x > priorPoint.x) {
          // Prior point is the outer corner so lower the current point to match.
          point.y = priorPoint.y;
        } else if (point.x < priorPoint.x) {
          // Current point is the outer corner so raise the prior point to match.
          priorPoint.y = point.y;
        }
      }
    }
  }

  private static List<PointF> ensureMinimumHorizontalDistanceBetweenVerticalEdgesForSide(
      PointF[] sidePoints, float minimumSideXDelta, boolean rightSide) {

    List<PointF> out = new ArrayList<>();
    Collections.addAll(out, sidePoints);

    // Iterate down half of the sides of the shape.
    for (int i = 0; i < out.size() / 2 - 1; i++) {
      int sideIndex = i * 2;
      PointF sideTop = out.get(sideIndex);
      PointF sideBottom = out.get(sideIndex + 1);
      PointF nextSideTop = out.get(sideIndex + 2);
      PointF nextSideBottom = out.get(sideIndex + 3);

      float sideXDelta = Math.abs(sideBottom.x - nextSideTop.x);
      if (sideXDelta < minimumSideXDelta) {
        // Remove the midpoints to join the side.
        out.remove(sideBottom);
        out.remove(nextSideTop);
        // Take the largest distance of the two remaining points.
        if (rightSide) {
          sideTop.x = nextSideBottom.x = Math.max(sideTop.x, nextSideBottom.x);
        } else {
          sideTop.x = nextSideBottom.x = Math.min(sideTop.x, nextSideBottom.x);
        }

        // Check the newly created side with its next neighbor by repeating this iteration if
        // there
        // are remaining neighbors still.
        i--;
      }
    }
    return out;
  }
}
