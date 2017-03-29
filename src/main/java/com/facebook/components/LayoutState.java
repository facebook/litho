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

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.displaylist.DisplayList;
import com.facebook.litho.displaylist.DisplayListException;
import com.facebook.litho.reference.BorderColorDrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;

import static android.content.Context.ACCESSIBILITY_SERVICE;
