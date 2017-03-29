/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import android.graphics.drawable.Drawable;
import android.support.v4.util.Pools;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;

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

    return sInstance;
  }

  private static PropsBuilder newBuilder(ComponentContext c, State state) {
    PropsBuilder builder = mBuilderPool.acquire();
    if (builder == null) {
      builder = new PropsBuilder();
    }

    builder.init(c, state);

    return builder;
  }

  public static PropsBuilder create(ComponentContext c) {
    return newBuilder(c, new State());
  }

  @Override
  protected Drawable onAcquire(
      ComponentContext context,
      Reference reference) {
    Drawable drawable = mDrawableResourcesCache.get(
        ((State) reference).mResId,
        context.getResources());
    return drawable;
  }

  @Override
  protected void onRelease(
      ComponentContext context,
      Drawable drawable,
      Reference reference) {
    mDrawableResourcesCache.release(drawable, ((State) reference).mResId);
  }

  private static class State extends Reference<Drawable> {
    int mResId;
