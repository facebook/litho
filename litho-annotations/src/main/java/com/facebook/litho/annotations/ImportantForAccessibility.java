/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.IntDef;

import android.support.v4.view.ViewCompat;

@IntDef({
    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES,
    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO,
    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
})
@Retention(RetentionPolicy.SOURCE)
public @interface ImportantForAccessibility {}
