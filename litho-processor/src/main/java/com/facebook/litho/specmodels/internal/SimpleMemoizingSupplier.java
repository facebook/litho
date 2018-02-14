/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.specmodels.internal;

import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Very simple memoizing supplier. Doesn't protect from double-execution if called from multiple
 * threads.
 */
public class SimpleMemoizingSupplier<T> implements Supplier<T> {
  @Nullable private Supplier<T> mDelegate;
  @Nullable private T mCache = null;

  public SimpleMemoizingSupplier(Supplier<T> delegate) {
    mDelegate = delegate;
  }

  @Override
  @Nullable
  public T get() {
    Supplier<T> delegate = mDelegate;
    if (delegate != null) {
      mCache = delegate.get();
      mDelegate = null;
    }
    return mCache;
  }
}
