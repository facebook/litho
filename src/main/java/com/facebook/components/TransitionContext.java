/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.HashSet;

import android.support.v4.util.SimpleArrayMap;

/**
 * TransitionContext is unique per LayoutState and contains all the transitions defined
 * in a component tree.
 */
class TransitionContext {

  // User defined transitions
