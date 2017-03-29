/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.annotation.TargetApi;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;

import com.facebook.litho.Transition.TransitionListener;
import com.facebook.litho.TransitionManager.KeyStatus;
import com.facebook.litho.TransitionProperties.PropertySetHolder;
import com.facebook.litho.TransitionProperties.PropertyType;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.facebook.litho.Transition.TransitionType.APPEAR;
import static com.facebook.litho.Transition.TransitionType.CHANGE;
import static com.facebook.litho.Transition.TransitionType.DISAPPEAR;

/**
 * Holds all the defined {@link Transition}s for a given transition key.
 * It's also responsible to start, stop and resume those transitions on a target View.
 */
// TODO(t16207387): After finished iterating on this class, it needs tests.
@TargetApi(ICE_CREAM_SANDWICH)
class TransitionKeySet implements TransitionListener {

  interface TransitionKeySetListener {
    void onTransitionKeySetStart(String key, View view);
    void onTransitionKeySetStop(String key, View view);
    void onTransitionKeySetEnd(String key, View view);
  }

