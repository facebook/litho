/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.fresco;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import com.facebook.litho.ImageContent;
import com.facebook.litho.Touchable;
import com.facebook.litho.fresco.common.NoOpDrawable;
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

  private final Drawable mNoOpDrawable = new NoOpDrawable();

  private final DraweeHolder<DH> mDraweeHolder;

  public DraweeDrawable(Context context, DH draweeHierarchy) {
    super(null);

    setCurrent(mNoOpDrawable);
    mDraweeHolder = DraweeHolder.create(draweeHierarchy, context);
