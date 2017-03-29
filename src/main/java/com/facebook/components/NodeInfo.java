/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

import android.support.annotation.IntDef;
import android.util.SparseArray;

import com.facebook.infer.annotation.ThreadConfined;

/**
 * NodeInfo holds information that are set to the {@link InternalNode} and needs to be used
 * while mounting a {@link MountItem} in {@link MountState}.
 */
