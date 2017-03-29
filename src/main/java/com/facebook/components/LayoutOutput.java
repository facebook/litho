/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.graphics.Rect;
import android.support.annotation.IntDef;

import com.facebook.litho.displaylist.DisplayList;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

/**
 * The output of a layout pass for a given {@link Component}. It's used by
 * {@link MountState} to mount a component.
 */
class LayoutOutput implements Cloneable {
  public static final int TYPE_CONTENT = 0;
  public static final int TYPE_BACKGROUND = 1;
  public static final int TYPE_FOREGROUND = 2;
  public static final int TYPE_HOST = 3;
  public static final int TYPE_BORDER = 4;

  @IntDef({TYPE_CONTENT, TYPE_BACKGROUND, TYPE_FOREGROUND, TYPE_HOST, TYPE_BORDER})
  @Retention(RetentionPolicy.SOURCE)
  @interface LayoutOutputType {}

  public static final int STATE_UNKNOWN = 0;
  public static final int STATE_UPDATED = 1;
  public static final int STATE_DIRTY = 2;

  @IntDef({STATE_UPDATED, STATE_UNKNOWN, STATE_DIRTY})
  @Retention(RetentionPolicy.SOURCE)
  @interface UpdateState {}

  private NodeInfo mNodeInfo;
  private ViewNodeInfo mViewNodeInfo;
  private long mId;
  private Component<?> mComponent;
  private final Rect mBounds = new Rect();
  private int mHostTranslationX;
  private int mHostTranslationY;
  private int mFlags;
  private long mHostMarker;
