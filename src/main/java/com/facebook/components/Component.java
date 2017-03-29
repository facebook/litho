/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.concurrent.atomic.AtomicInteger;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;

import com.facebook.components.ComponentLifecycle.MountType;
import com.facebook.components.ComponentLifecycle.StateContainer;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
