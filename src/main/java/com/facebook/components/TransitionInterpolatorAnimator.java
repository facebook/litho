/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.animation.Interpolator;

import com.facebook.litho.Transition.TransitionAnimator;
import com.facebook.litho.Transition.TransitionListener;
import com.facebook.litho.TransitionProperties.PropertyChangeHolder;
