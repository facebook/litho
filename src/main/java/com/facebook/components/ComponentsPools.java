/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.util.Pools;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseArray;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.displaylist.DisplayList;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.yoga.YogaConfig;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaExperimentalFeature;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaNodeAPI;
import com.facebook.yoga.CSSNodeDEPRECATED;
import com.facebook.yoga.Spacing;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

/**
 * Pools of recycled resources.
 *
 * FUTURE: Consider customizing the pool implementation such that we can match buffer sizes. Without
 * this we will tend to expand all buffers to the largest size needed.
 */
public class ComponentsPools {

  private static YogaConfig sYogaConfig;

  private static final int SCRAP_ARRAY_INITIAL_SIZE = 4;

  private ComponentsPools() {
  }

  // FUTURE: tune pool max sizes

  private static final Object mountContentLock = new Object();

  private static final Pools.SynchronizedPool<LayoutState> sLayoutStatePool =
      new Pools.SynchronizedPool<>(64);

  private static final Pools.SynchronizedPool<InternalNode> sInternalNodePool =
      new Pools.SynchronizedPool<>(256);

  private static final Pools.SynchronizedPool<NodeInfo> sNodeInfoPool =
      new Pools.SynchronizedPool<>(256);

  private static final Pools.SynchronizedPool<ViewNodeInfo> sViewNodeInfoPool =
      new Pools.SynchronizedPool<>(64);

  private static final Pools.SynchronizedPool<YogaNodeAPI> sYogaNodePool =
      new Pools.SynchronizedPool<>(256);

