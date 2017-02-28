// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

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
