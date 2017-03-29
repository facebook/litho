// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.ColorDrawable;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowDrawable;

import static org.robolectric.internal.Shadow.directlyOn;

/**
 * Shadows a {@link ColorDrawable} to support of drawing its description on a
 * {@link org.robolectric.shadows.ShadowCanvas}
 */
@Implements(value = ColorDrawable.class, inheritImplementationMethods = true)
