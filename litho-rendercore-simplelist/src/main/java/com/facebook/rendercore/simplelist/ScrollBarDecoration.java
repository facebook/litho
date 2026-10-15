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

package com.facebook.rendercore.simplelist;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ScrollBarDecoration extends RecyclerView.ItemDecoration {

  private final Paint mPaint = new Paint();
  private final RectF mBounds = new RectF();
  private final RectF mBackgroundBounds = new RectF();

  private final int mScrollBarSize;
  private final int mColor;
  private final int mBackgroundColor;
  private final int mScrollBarTargetAlpha;
  private final boolean mFlashOnFirstDraw;
  private final int mBackgroundTargetAlpha;
  private float mScrollBarAlpha;
  private final int mCornerRadius;
  // Previously we had only one "margin" property for the collection. The margin logic was depended
  // on collection orientation: left&right for vertical orientation, top&bottom for horizontal.
  // Inset margins allows users to set up each margin independently, no matter of the orientation.
  // RTL notes:
  // mMargins.left stands for start margin, mMargins.right stands for end margin, but
  // mBounds.left && mBackgroundBounds.left is always left, since they're always drawn left to right
  private final Rect mMargins;
  private final boolean mUseInsetMargins;
  private final Shadow mShadow;

  @Nullable private OnFirstDrawListener mOnFirstDrawListener;
  boolean mDidDrawOnce = false;

  public ScrollBarDecoration(
      int scrollBarColor,
      int scrollBarBackgroundColor,
      int scrollBarSize,
      int cornerRadius,
      boolean flashOnFirstDraw,
      Rect margins,
      boolean useInsetMargins,
      Shadow shadow) {
    mBackgroundColor = scrollBarBackgroundColor;
    mColor = scrollBarColor;
    mScrollBarTargetAlpha = Color.alpha(scrollBarColor);
    mBackgroundTargetAlpha = Color.alpha(mBackgroundColor);
    mScrollBarSize = scrollBarSize;
    mCornerRadius = cornerRadius;
    mFlashOnFirstDraw = flashOnFirstDraw;
    if (flashOnFirstDraw) {
      mScrollBarAlpha = 1f;
    }
    mMargins = margins;
    mUseInsetMargins = useInsetMargins;
    mShadow = shadow;
  }

  public void setOnFirstDrawListener(OnFirstDrawListener onFirstDrawListener) {
    mOnFirstDrawListener = onFirstDrawListener;
  }

  public interface OnFirstDrawListener {
    void onFirstDraw(RecyclerView recyclerView);
  }

  public static class Shadow {
    private final float mShadowOffsetWidth;
    private final float mShadowOffsetHeight;
    private final float mShadowOpacity;
    private final float mShadowRadius;
    private final int mShadowColor;
    private final int mShadowColorWithAlpha;

    public Shadow(
        float shadowOffsetWidth,
        float shadowOffsetHeight,
        float shadowOpacity,
        float shadowRadius,
        int shadowColor) {
      mShadowOffsetWidth = shadowOffsetWidth;
      mShadowOffsetHeight = shadowOffsetHeight;
      mShadowOpacity = shadowOpacity;
      mShadowRadius = shadowRadius;
      mShadowColor = shadowColor;
      mShadowColorWithAlpha = computeShadowColorWithAlpha();
    }

    @ColorInt
    @SuppressLint("ColorRGBUsageIssue")
    private int computeShadowColorWithAlpha() {
      final int alpha = (int) (mShadowOpacity * Color.alpha(mShadowColor));
      return Color.argb(
          alpha, Color.red(mShadowColor), Color.green(mShadowColor), Color.blue(mShadowColor));
    }
  }

  @Override
  public void onDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.State state) {

    super.onDrawOver(c, recyclerView, state);
    if (mOnFirstDrawListener != null && !mDidDrawOnce) {
      mOnFirstDrawListener.onFirstDraw(recyclerView);
    }
    mDidDrawOnce = true;
    if (mScrollBarAlpha == 0f) {
      return;
    }

    final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
    if (layoutManager instanceof LinearLayoutManager) {
      final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
      boolean isRtl = linearLayoutManager.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;

      if (linearLayoutManager.getOrientation() == LinearLayoutManager.VERTICAL) {
        final int extent = linearLayoutManager.computeVerticalScrollExtent(state);
        final int range = linearLayoutManager.computeVerticalScrollRange(state);
        int size = recyclerView.getHeight();
        if (mUseInsetMargins) {
          // decrease height by vertical margins
          size = size - mMargins.top - mMargins.bottom;
        }
        final int offset = linearLayoutManager.computeVerticalScrollOffset(state);
        if (range == extent) {
          // There is no scroll available so we should not draw the scrollbar.
          return;
        }

        int length = Math.round((float) size * extent / range);
        if (length > size / 2) {
          length = size / 2;
        }
        int drawOffset = Math.round((float) (size - length) * offset / (range - extent));

        if (isRtl) {
          // scroll indicator is attached to the left side, shift it to the right by end margin
          mBounds.left = mMargins.right; // left += end
          mBounds.right = mScrollBarSize + mMargins.right; // right += end
        } else {
          // scroll indicator is attached to the right side, shift it to the left by end margin
          mBounds.left = recyclerView.getWidth() - mScrollBarSize - mMargins.right; // left -= end
          mBounds.right = recyclerView.getWidth() - mMargins.right; // right -= end
        }
        mBounds.top = drawOffset;
        mBounds.bottom = drawOffset + length;

        mBackgroundBounds.left = mBounds.left;
        mBackgroundBounds.right = mBounds.right;
        mBackgroundBounds.top = 0;
        mBackgroundBounds.bottom = recyclerView.getHeight();

        if (mUseInsetMargins) {
          // shift indicator's vertical bounds by top margin
          mBounds.top += mMargins.top;
          mBounds.bottom += mMargins.top;
          // simply apply vertical margins for background
          mBackgroundBounds.top += mMargins.top;
          mBackgroundBounds.bottom -= mMargins.bottom;
        }
      } else {
        // Copy all of the above code but for horizontal orientation.
        final int extent = linearLayoutManager.computeHorizontalScrollExtent(state);
        final int range = linearLayoutManager.computeHorizontalScrollRange(state);
        int size = recyclerView.getWidth();
        if (mUseInsetMargins) {
          // decrease width by horizontal margins
          size = size - mMargins.left - mMargins.right;
        }
        final int offset = linearLayoutManager.computeHorizontalScrollOffset(state);
        if (range == extent) {
          // There is no scroll available so we should not draw the scrollbar.
          return;
        }

        int length = Math.round((float) size * extent / range);
        if (length > size / 2) {
          length = size / 2;
        }
        int drawOffset = Math.round((float) (size - length) * offset / (range - extent));

        mBounds.left = drawOffset;
        mBounds.right = drawOffset + length;
        // scroll indicator is attached to the bottom, shift it to the top by bottom margin
        mBounds.top = recyclerView.getHeight() - mScrollBarSize - mMargins.bottom;
        mBounds.bottom = recyclerView.getHeight() - mMargins.bottom;

        mBackgroundBounds.left = 0;
        mBackgroundBounds.top = mBounds.top;
        mBackgroundBounds.right = recyclerView.getWidth();
        mBackgroundBounds.bottom = mBounds.bottom;

        if (mUseInsetMargins) {
          if (isRtl) {
            // shift indicator to the right by end margin
            mBounds.left += mMargins.right; // left += end
            mBounds.right += mMargins.right; // right += end
            // simply apply horizontal margins for background
            mBackgroundBounds.left += mMargins.right; // left += end
            mBackgroundBounds.right -= mMargins.left; // right -= start
          } else {
            // shift indicator to the right by start margin
            mBounds.left += mMargins.left; // left += start
            mBounds.right += mMargins.left; // right += start
            // simply apply horizontal margins for background
            mBackgroundBounds.left += mMargins.left; // left += start
            mBackgroundBounds.right -= mMargins.right; // right -= end
          }
        }
      }

      // We are drawing a simple rectangle here. We would really want any
      // drawable to be passed instead.
      if (mBackgroundColor != Color.TRANSPARENT) {
        final boolean drawShadow = mShadow.mShadowRadius != 0f && c.isHardwareAccelerated();
        if (drawShadow) {
          mPaint.setShadowLayer(
              mShadow.mShadowRadius,
              mShadow.mShadowOffsetWidth,
              mShadow.mShadowOffsetHeight,
              mShadow.mShadowColorWithAlpha);
        }
        mPaint.setColor(mBackgroundColor);
        mPaint.setAlpha((int) (mScrollBarAlpha * mBackgroundTargetAlpha));
        c.drawRoundRect(mBackgroundBounds, mCornerRadius, mCornerRadius, mPaint);
        if (drawShadow) {
          mPaint.clearShadowLayer();
        }
      }

      mPaint.setColor(mColor);
      mPaint.setAlpha((int) (mScrollBarAlpha * mScrollBarTargetAlpha));
      c.drawRoundRect(mBounds, mCornerRadius, mCornerRadius, mPaint);
    }
  }

  public void setScrollBarAlpha(float scrollBarAlpha) {
    mScrollBarAlpha = scrollBarAlpha;
  }

  public float getScrollBarAlpha() {
    return mScrollBarAlpha;
  }

  public static boolean isEquivalent(
      ScrollBarDecoration scrollBarDecoration,
      int color,
      int backgroundColor,
      int size,
      int cornerRadius,
      boolean flashScrollIndicator,
      Rect margins,
      boolean useInsetMargins,
      Shadow shadow) {
    return scrollBarDecoration.mColor == color
        && scrollBarDecoration.mBackgroundColor == backgroundColor
        && scrollBarDecoration.mScrollBarSize == size
        && scrollBarDecoration.mCornerRadius == cornerRadius
        && scrollBarDecoration.mFlashOnFirstDraw == flashScrollIndicator
        && scrollBarDecoration.mMargins.equals(margins)
        && scrollBarDecoration.mUseInsetMargins == useInsetMargins
        && scrollBarDecoration.mShadow.mShadowOffsetWidth == shadow.mShadowOffsetWidth
        && scrollBarDecoration.mShadow.mShadowOffsetHeight == shadow.mShadowOffsetHeight
        && scrollBarDecoration.mShadow.mShadowOpacity == shadow.mShadowOpacity
        && scrollBarDecoration.mShadow.mShadowRadius == shadow.mShadowRadius
        && scrollBarDecoration.mShadow.mShadowColor == shadow.mShadowColor;
  }
}
