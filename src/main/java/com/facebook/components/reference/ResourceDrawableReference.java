/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.reference;

import android.graphics.drawable.Drawable;
import android.support.v4.util.Pools;

import com.facebook.components.ComponentContext;
import com.facebook.components.config.ComponentsConfiguration;

/**
 * A Reference used to acquire {@link Drawable} defined as resources. This uses an internal cache to
 * avoid recreating the same Drawable instances multiple times.
 */
public final class ResourceDrawableReference extends ReferenceLifecycle<Drawable> {

  private static ResourceDrawableReference sInstance;

  private static final Pools.SynchronizedPool<PropsBuilder> mBuilderPool =
      new Pools.SynchronizedPool<PropsBuilder>(2);

  private final DrawableResourcesCache mDrawableResourcesCache;

  private ResourceDrawableReference() {
    mDrawableResourcesCache = new DrawableResourcesCache();
  }

  public static synchronized ResourceDrawableReference get() {
    if (sInstance == null) {
      sInstance = new ResourceDrawableReference();
    }
