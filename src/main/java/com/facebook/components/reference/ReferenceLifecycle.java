/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import android.support.v4.util.Pools;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.Diff;

/**
 * ReferenceLifecycle objects which are able retreive resources at runtime without needing to keep
 * them constantly in memory. References should be used any time it's necessary to include a large
 * Object into a {@link com.facebook.litho.Component} in order to limit the amount of
 * retained memory in ComponentTree.
 *
