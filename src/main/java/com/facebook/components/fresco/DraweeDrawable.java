/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.fresco;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import com.facebook.components.ImageContent;
import com.facebook.components.Touchable;
import com.facebook.components.fresco.common.NoOpDrawable;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.view.DraweeHolder;

/**
 * A Drawable that draws images using Drawee.
 */
public class DraweeDrawable<DH extends DraweeHierarchy>
    extends ForwardingDrawable
    implements ImageContent, Touchable {

