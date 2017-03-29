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
import java.util.LinkedList;
import java.util.List;

import android.annotation.TargetApi;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.util.Property;
import android.view.View;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.facebook.litho.TransitionProperties.PropertyType.ALPHA;
import static com.facebook.litho.TransitionProperties.PropertyType.NONE;
import static com.facebook.litho.TransitionProperties.PropertyType.TRANSLATION_X;
import static com.facebook.litho.TransitionProperties.PropertyType.TRANSLATION_Y;
