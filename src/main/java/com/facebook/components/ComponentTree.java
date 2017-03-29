/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.GuardedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.IntDef;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.ViewParent;

import com.facebook.infer.annotation.ReturnsOwnership;

import static com.facebook.components.ComponentLifecycle.StateUpdate;
import static com.facebook.components.ComponentsLogger.ACTION_SUCCESS;
import static com.facebook.components.ComponentsLogger.EVENT_LAYOUT_CALCULATE;
import static com.facebook.components.ComponentsLogger.EVENT_PRE_ALLOCATE_MOUNT_CONTENT;
import static com.facebook.components.ComponentsLogger.PARAM_IS_BACKGROUND_LAYOUT;
import static com.facebook.components.ComponentsLogger.PARAM_LOG_TAG;
import static com.facebook.components.ComponentsLogger.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.components.ThreadUtils.assertHoldsLock;
import static com.facebook.components.ThreadUtils.assertMainThread;
