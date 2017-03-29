/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.support.v4.util.Pools;
import android.util.StateSet;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

/**
 * A cache that holds Drawables retreived from Android {@link android.content.res.Resources} for
