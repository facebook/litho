/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.View;

import com.facebook.components.TransitionProperties.PropertyChangeHolder;
import com.facebook.components.TransitionProperties.PropertySetHolder;
import com.facebook.components.TransitionProperties.PropertyType;

import static com.facebook.components.Transition.TransitionType.APPEAR;
import static com.facebook.components.Transition.TransitionType.CHANGE;
import static com.facebook.components.Transition.TransitionType.DISAPPEAR;
import static com.facebook.components.Transition.TransitionType.UNDEFINED;

/**
 * A Transition is an animation running on a Component or ComponentLayout with the matching
 * transitionKey {@link com.facebook.components.ComponentLayout.Builder#transitionKey(String)}.
 */
public class Transition {

  interface TransitionListener {
    void onTransitionEnd();
  }

  interface TransitionAnimator<T extends TransitionAnimator<? super T>> extends Cloneable {
