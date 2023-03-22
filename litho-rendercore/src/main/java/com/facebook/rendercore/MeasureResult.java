/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;

import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.utils.MeasureSpecUtils;

/** Encapsulates the measured size of a Mountable, and any layout data */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class MeasureResult {

  public final int width;
  public final int height;
  public final @Nullable Object layoutData;
  public final boolean mHadExceptions;

  public MeasureResult(final int width, final int height, final @Nullable Object layoutData) {
    this.width = width;
    this.height = height;
    this.layoutData = layoutData;
    this.mHadExceptions = false;
  }

  public MeasureResult(final int width, final int height) {
    this(width, height, null);
  }

  /** This constructor should only be used if there were exceptions during measurement. */
  private MeasureResult() {
    this.width = 0;
    this.height = 0;
    this.layoutData = null;
    this.mHadExceptions = true;
  }

  /**
   * Returns a {@link MeasureResult} with sizes set based on the provided {@param widthSpec} and
   * {@param heightSpec}.
   *
   * <p>This method should only be used for Mountable Components which do not measure themselves -
   * it's the parent that has determined the exact size for this child.
   *
   * @throws IllegalArgumentException if the widthSpec or heightSpec is not exact
   */
  public static MeasureResult fromSpecs(final int widthSpec, final int heightSpec) {
    if (MeasureSpecUtils.getMode(widthSpec) != EXACTLY
        || MeasureSpecUtils.getMode(heightSpec) != EXACTLY) {
      throw new IllegalArgumentException(
          "The sizes must be exact, but width is "
              + MeasureSpecUtils.getMeasureSpecDescription(widthSpec)
              + " and height is "
              + MeasureSpecUtils.getMeasureSpecDescription(heightSpec));
    }
    return new MeasureResult(
        MeasureSpecUtils.getSize(widthSpec), MeasureSpecUtils.getSize(heightSpec));
  }

  /**
   * Returns a {@link MeasureResult} to respect both size specs and try to keep both width and
   * height equal. This will only not guarantee equal width and height if these specs use modes and
   * sizes which prevent it.
   */
  public static MeasureResult withEqualDimensions(
      final int widthSpec, final int heightSpec, final @Nullable Object layoutData) {
    final int widthMode = View.MeasureSpec.getMode(widthSpec);
    final int widthSize = View.MeasureSpec.getSize(widthSpec);
    final int heightMode = View.MeasureSpec.getMode(heightSpec);
    final int heightSize = View.MeasureSpec.getSize(heightSpec);

    if (widthMode == UNSPECIFIED && heightMode == UNSPECIFIED) {
      return new MeasureResult(0, 0, layoutData);
    }

    final int width;
    final int height;

    if (widthMode == EXACTLY) {
      width = widthSize;

      switch (heightMode) {
        case EXACTLY:
          height = heightSize;
          break;
        case AT_MOST:
          height = Math.min(widthSize, heightSize);
          break;
        case UNSPECIFIED:
        default:
          height = widthSize;
          break;
      }
    } else if (widthMode == AT_MOST) {
      switch (heightMode) {
        case EXACTLY:
          height = heightSize;
          width = Math.min(widthSize, heightSize);
          break;
        case AT_MOST:
          // if both are AT_MOST, choose the smaller one to keep width and height equal
          final int chosenSize = Math.min(widthSize, heightSize);
          width = chosenSize;
          height = chosenSize;
          break;
        case UNSPECIFIED:
        default:
          width = widthSize;
          height = widthSize;
          break;
      }
    } else {
      width = 0;
      height = 0;
    }

    return new MeasureResult(width, height);
  }

  /**
   * Returns a {@link MeasureResult} that respects both specs and the desired width and height. The
   * desired size is usually the necessary pixels to render the inner content.
   */
  public static MeasureResult withDesiredPx(
      final int widthSpec,
      final int heightSpec,
      final int desiredWidthPx,
      final int desiredHeightPx) {
    return new MeasureResult(
        getResultSizePxWithSpecAndDesiredPx(widthSpec, desiredWidthPx),
        getResultSizePxWithSpecAndDesiredPx(heightSpec, desiredHeightPx));
  }

  /**
   * Returns a {@link MeasureResult} that respects both specs and the desired width and height. The
   * desired size is usually the necessary pixels to render the inner content.
   */
  public static MeasureResult withDesiredPx(
      int widthSpec,
      int heightSpec,
      final int desiredWidthPx,
      final int desiredHeightPx,
      final @Nullable Object layoutData) {
    return new MeasureResult(
        getResultSizePxWithSpecAndDesiredPx(widthSpec, desiredWidthPx),
        getResultSizePxWithSpecAndDesiredPx(heightSpec, desiredHeightPx),
        layoutData);
  }

  private static int getResultSizePxWithSpecAndDesiredPx(int spec, int desiredSize) {
    final int mode = MeasureSpecUtils.getMode(spec);
    switch (mode) {
      case UNSPECIFIED:
        return desiredSize;
      case AT_MOST:
        return Math.min(MeasureSpecUtils.getSize(spec), desiredSize);
      case EXACTLY:
        return MeasureSpecUtils.getSize(spec);
      default:
        throw new IllegalStateException("Unexpected size spec mode");
    }
  }

  /**
   * Measure according to an aspect ratio an width and height constraints. This version of
   * forAspectRatio will respect the intrinsic size of the component being measured.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   * @param intrinsicWidth A pixel value for the intrinsic width of the measured component
   * @param intrinsicHeight A pixel value for the intrinsic height of the measured component
   * @param aspectRatio The aspect ration size against
   */
  public static MeasureResult forAspectRatio(
      int widthSpec, int heightSpec, int intrinsicWidth, int intrinsicHeight, float aspectRatio) {
    if (MeasureSpecUtils.getMode(widthSpec) == AT_MOST
        && MeasureSpecUtils.getSize(widthSpec) > intrinsicWidth) {

      widthSpec = MeasureSpecUtils.atMost(intrinsicWidth);
    }

    if (MeasureSpecUtils.getMode(heightSpec) == AT_MOST
        && MeasureSpecUtils.getSize(heightSpec) > intrinsicHeight) {

      heightSpec = MeasureSpecUtils.atMost(intrinsicHeight);
    }

    return forAspectRatio(widthSpec, heightSpec, aspectRatio);
  }

  /**
   * Measure according to an aspect ratio an width and height constraints.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   * @param aspectRatio The aspect ration size against
   */
  public static MeasureResult forAspectRatio(int widthSpec, int heightSpec, float aspectRatio) {
    if (aspectRatio < 0) {
      throw new IllegalArgumentException("The aspect ratio must be a positive number");
    }

    final int widthMode = MeasureSpecUtils.getMode(widthSpec);
    final int widthSize = MeasureSpecUtils.getSize(widthSpec);
    final int heightMode = MeasureSpecUtils.getMode(heightSpec);
    final int heightSize = MeasureSpecUtils.getSize(heightSpec);
    final int widthBasedHeight = (int) Math.ceil(widthSize / aspectRatio);
    final int heightBasedWidth = (int) Math.ceil(heightSize * aspectRatio);

    int outputWidth = 0;
    int outputHeight = 0;

    if (widthMode == UNSPECIFIED && heightMode == UNSPECIFIED) {
      // default to size {0, 0} because both width and height are UNSPECIFIED
      return new MeasureResult(0, 0);
    }

    // Both modes are AT_MOST, find the largest possible size which respects both constraints.
    if (widthMode == AT_MOST && heightMode == AT_MOST) {
      if (widthBasedHeight > heightSize) {
        outputWidth = heightBasedWidth;
        outputHeight = heightSize;
      } else {
        outputWidth = widthSize;
        outputHeight = widthBasedHeight;
      }
    }
    // Width is set to exact measurement and the height is either unspecified or is allowed to be
    // large enough to accommodate the given aspect ratio.
    else if (widthMode == EXACTLY) {
      outputWidth = widthSize;

      if (heightMode == UNSPECIFIED || widthBasedHeight <= heightSize) {
        outputHeight = widthBasedHeight;
      } else {
        outputHeight = heightSize;
      }
    }
    // Height is set to exact measurement and the width is either unspecified or is allowed to be
    // large enough to accommodate the given aspect ratio.
    else if (heightMode == EXACTLY) {
      outputHeight = heightSize;

      if (widthMode == UNSPECIFIED || heightBasedWidth <= widthSize) {
        outputWidth = heightBasedWidth;
      } else {
        outputWidth = widthSize;
      }
    }
    // Width is set to at most measurement. If that is the case heightMode must be unspecified.
    else if (widthMode == AT_MOST) {
      outputWidth = widthSize;
      outputHeight = widthBasedHeight;
    }
    // Height is set to at most measurement. If that is the case widthMode must be unspecified.
    else if (heightMode == AT_MOST) {
      outputWidth = heightBasedWidth;
      outputHeight = heightSize;
    }

    return new MeasureResult(outputWidth, outputHeight);
  }

  /**
   * Returns a {@link MeasureResult} with sizes set based on the provided {@param widthSpec} and
   * {@param heightSpec} if the spec mode is EXACTLY or AT_MOST, otherwise uses fallback value.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   */
  public static MeasureResult fillSpaceOrGone(
      final int widthSpec, final int heightSpec, final @Nullable Object layoutData) {
    return fillSpace(widthSpec, heightSpec, 0, 0, layoutData);
  }

  /**
   * Returns a {@link MeasureResult} with sizes set based on the provided {@param widthSpec} and
   * {@param heightSpec} if the spec mode is EXACTLY or AT_MOST, otherwise uses fallback value.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   * @param widthFallback The width value for the UNSPECIFIED mode
   * @param heightFallback The height value for the UNSPECIFIED mode
   */
  public static MeasureResult fillSpace(
      final int widthSpec,
      final int heightSpec,
      final int widthFallback,
      final int heightFallback,
      final @Nullable Object layoutData) {
    final int widthMode = View.MeasureSpec.getMode(widthSpec);
    final int widthSize = View.MeasureSpec.getSize(widthSpec);
    final int heightMode = View.MeasureSpec.getMode(heightSpec);
    final int heightSize = View.MeasureSpec.getSize(heightSpec);

    if (widthMode == UNSPECIFIED && heightMode == UNSPECIFIED) {
      return new MeasureResult(widthFallback, heightFallback, layoutData);
    }

    final int width;
    final int height;

    switch (widthMode) {
      case EXACTLY:
      case AT_MOST:
        width = widthSize;
        break;
      case UNSPECIFIED:
      default:
        width = widthFallback;
        break;
    }

    switch (heightMode) {
      case EXACTLY:
      case AT_MOST:
        height = heightSize;
        break;
      case UNSPECIFIED:
      default:
        height = heightFallback;
        break;
    }

    return new MeasureResult(width, height, layoutData);
  }

  public static MeasureResult error() {
    return new MeasureResult();
  }

  @Override
  public String toString() {
    return "MeasureResult:[width "
        + width
        + " height "
        + height
        + " layoutData "
        + layoutData
        + " mHadExceptions "
        + mHadExceptions
        + "]";
  }
}
