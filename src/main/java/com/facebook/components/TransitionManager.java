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
import java.util.HashSet;
import java.util.List;

import android.annotation.TargetApi;
import android.support.annotation.IntDef;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;
import android.view.ViewParent;

import com.facebook.litho.TransitionKeySet.TransitionKeySetListener;
