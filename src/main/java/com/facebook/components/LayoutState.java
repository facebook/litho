/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.facebook.components.config.ComponentsConfiguration;
import com.facebook.components.displaylist.DisplayList;
import com.facebook.components.displaylist.DisplayListException;
import com.facebook.components.reference.BorderColorDrawableReference;
import com.facebook.components.reference.Reference;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.components.Component.isHostSpec;
import static com.facebook.components.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.components.Component.isMountSpec;
import static com.facebook.components.Component.isMountViewSpec;
import static com.facebook.components.ComponentContext.NULL_LAYOUT;
import static com.facebook.components.ComponentLifecycle.MountType.NONE;
import static com.facebook.components.ComponentsLogger.ACTION_SUCCESS;
import static com.facebook.components.ComponentsLogger.EVENT_COLLECT_RESULTS;
import static com.facebook.components.ComponentsLogger.EVENT_CREATE_LAYOUT;
import static com.facebook.components.ComponentsLogger.EVENT_CSS_LAYOUT;
import static com.facebook.components.ComponentsLogger.PARAM_LOG_TAG;
import static com.facebook.components.ComponentsLogger.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.components.MountItem.FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.components.MountState.ROOT_HOST_ID;
import static com.facebook.components.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.components.SizeSpec.EXACTLY;

