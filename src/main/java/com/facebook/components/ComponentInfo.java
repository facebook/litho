/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.support.v4.util.Pools;
import android.support.v4.util.Pools.Pool;
import android.support.v4.util.SimpleArrayMap;

/**
 * Keeps the {@link Component} and its information that will allow the framework
 * to understand how to render it.
 *
 * SpanSize will be defaulted to 1. It is the information that is required to calculate
 * how much of the SpanCount the component should occupy in a Grid layout.
 *
 * IsSticky will be defaulted to false. It determines if the component should be
 * a sticky header or not
 */
public class ComponentInfo {

  private static final Pool<Builder> sBuilderPool = new Pools.SynchronizedPool<>(2);
  private static final Pool<ComponentInfo> sComponentInfoPool = new Pools.SynchronizedPool<>(8);
  private static final String IS_STICKY = "is_sticky";
  private static final String SPAN_SIZE = "span_size";

  private Component mComponent;
