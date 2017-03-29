/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import java.util.List;

import android.support.v4.util.SparseArrayCompat;

import com.facebook.litho.ComponentTree;

/**
 * BinderTreeCollection hide the structure used to operate on ComponentTrees used by the Binder.
 * Right now we are operating on a SparseArray.
 * Shifting the SparseArray left and right is potentially bad. Each call uses a System.arraycopy()
 * this means shiftSparseArrayRight/Left is O(n*m), where n is (sparseArray.size() - fromPosition)
