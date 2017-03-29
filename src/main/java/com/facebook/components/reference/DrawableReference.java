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

import com.facebook.litho.ComponentContext;

/**
 * A very simple Reference for {@link Drawable} used in all the cases where it's not
 * possible/desirable to use a real Reference. This will simply keep a reference to the Drawable
 * in the Props and return it. Please take care when using this. It keeps the drawable in memory
 * all the time and should only be used when the other built in specs are not applicable and
 * it's not possible to write a custom ReferenceSpec
 */
public final class DrawableReference extends ReferenceLifecycle<Drawable> {

  private static DrawableReference sInstance;

  private DrawableReference() {

  }

  public static synchronized DrawableReference get() {
    if (sInstance == null) {
      sInstance = new DrawableReference();
    }
    return sInstance;
  }

  public static PropsBuilder create() {
    return new PropsBuilder(new State());
  }

  @Override
  protected Drawable onAcquire(
      ComponentContext context,
