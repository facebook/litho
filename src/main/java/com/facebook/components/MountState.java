/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

import com.facebook.R;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.reference.Reference;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.Component.isMountViewSpec;
import static com.facebook.litho.ComponentHostUtils.maybeInvalidateAccessibilityState;
import static com.facebook.litho.ComponentHostUtils.maybeSetDrawableState;
import static com.facebook.litho.ComponentsLogger.ACTION_SUCCESS;
import static com.facebook.litho.ComponentsLogger.EVENT_MOUNT;
import static com.facebook.litho.ComponentsLogger.EVENT_PREPARE_MOUNT;
import static com.facebook.litho.ComponentsLogger.EVENT_SHOULD_UPDATE_REFERENCE_LAYOUT_MISMATCH;
import static com.facebook.litho.ComponentsLogger.PARAM_IS_DIRTY;
import static com.facebook.litho.ComponentsLogger.PARAM_LOG_TAG;
import static com.facebook.litho.ComponentsLogger.PARAM_MOUNTED_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_MOVED_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_NO_OP_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_UNCHANGED_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_UNMOUNTED_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_UPDATED_COUNT;
import static com.facebook.litho.ThreadUtils.assertMainThread;

/**
 * Encapsulates the mounted state of a {@link Component}. Provides APIs to update state
 * by recycling existing UI elements e.g. {@link Drawable}s.
 *
 * @see #mount(LayoutState, Rect)
 * @see ComponentView
 * @see LayoutState
 */
class MountState {

  static final int ROOT_HOST_ID = 0;
