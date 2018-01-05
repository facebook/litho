/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.drawable.Drawable;
import android.support.annotation.Px;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaDirection;

/**
 * <p>Represents a {@link Component}'s computed layout state. The computed bounds will be used by
 * the framework to define the size and position of the component's mounted {@link
 * android.view.View}s and {@link android.graphics.drawable.Drawable}s returned. by {@link
 * ComponentLifecycle#mount(ComponentContext, Object)}.
 */
@ThreadConfined(ThreadConfined.ANY)
public interface ComponentLayout {

  @Px
  int getX();

  @Px
  int getY();

  @Px
  int getWidth();

  @Px
  int getHeight();

  @Px
  int getPaddingTop();

  @Px
  int getPaddingRight();

  @Px
  int getPaddingBottom();

  @Px
  int getPaddingLeft();

  boolean isPaddingSet();

  Reference<? extends Drawable> getBackground();

  YogaDirection getResolvedLayoutDirection();
}
