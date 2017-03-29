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
import java.util.List;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.View;

import com.facebook.litho.TransitionProperties.PropertyChangeHolder;
import com.facebook.litho.TransitionProperties.PropertySetHolder;
import com.facebook.litho.TransitionProperties.PropertyType;

