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

package com.facebook.litho.widget;

import android.content.Context;
import androidx.annotation.IntDef;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.annotation.Nullable;

/** Utility class containing snapping related behavior of {@link RecyclerView}. */
public class SnapUtil {

  /* No snap helper is required */
  public static final int SNAP_NONE = Integer.MIN_VALUE;
  public static final int SNAP_TO_END = LinearSmoothScroller.SNAP_TO_END;
  /* This snap mode will cause a StartSnapHelper to be used */
  public static final int SNAP_TO_START = LinearSmoothScroller.SNAP_TO_START;
  /* This snap mode will cause a PagerSnapHelper to be used */
  public static final int SNAP_TO_CENTER = Integer.MAX_VALUE;
  /* This snap mode will cause a LinearSnapHelper to be used */
  public static final int SNAP_TO_CENTER_CHILD = Integer.MAX_VALUE - 1;

  /* This snap mode will cause a custom LinearSnapHelper to be used */
  public static final int SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED = Integer.MAX_VALUE - 2;

  /* The default fling offset for StartSnapHelper */
  public static final int SNAP_TO_START_DEFAULT_FLING_OFFSET = 1;

  @IntDef({
    SNAP_NONE,
    SNAP_TO_END,
    SNAP_TO_START,
    SNAP_TO_CENTER,
    SNAP_TO_CENTER_CHILD,
    SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface SnapMode {}

  @Nullable
  public static SnapHelper getSnapHelper(
      @SnapMode int snapMode, int deltaJumpThreshold, int startSnapFlingOffset) {
    switch (snapMode) {
      case SNAP_TO_CENTER:
        return new PagerSnapHelper();
      case SNAP_TO_START:
        return new StartSnapHelper(startSnapFlingOffset);
      case SNAP_TO_CENTER_CHILD:
        return new LinearSnapHelper();
      case SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED:
        return new CustomSpeedLinearSnapHelper(deltaJumpThreshold);
      case SNAP_TO_END:
      case SNAP_NONE:
      default:
        return null;
    }
  }

  /**
   * @return {@link androidx.recyclerview.widget.RecyclerView.SmoothScroller} that takes snapping
   *     into account.
   */
  public static RecyclerView.SmoothScroller getSmoothScrollerWithOffset(
      Context context, final int offset, final SmoothScrollAlignmentType type) {
    if (type == SmoothScrollAlignmentType.SNAP_TO_ANY
        || type == SmoothScrollAlignmentType.SNAP_TO_START
        || type == SmoothScrollAlignmentType.SNAP_TO_END) {
      final int snapPreference = type.getValue();
      return new EdgeSnappingSmoothScroller(context, snapPreference, offset);
    } else if (type == SmoothScrollAlignmentType.SNAP_TO_CENTER) {
      return new CenterSnappingSmoothScroller(context, offset);
    } else {
      return new LinearSmoothScroller(context);
    }
  }

  public static int getSnapModeFromString(@Nullable String snapString) {
    if (snapString == null) {
      return SNAP_NONE;
    }
    switch (snapString) {
      case "SNAP_TO_END":
        return SNAP_TO_END;
      case "SNAP_TO_START":
        return SNAP_TO_START;
      case "SNAP_TO_CENTER":
        return SNAP_TO_CENTER;
      case "SNAP_TO_CENTER_CHILD":
        return SNAP_TO_CENTER_CHILD;
      case "SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED":
        return SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED;
      case "SNAP_NONE":
      default:
        return SNAP_NONE;
    }
  }
}
