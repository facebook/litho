/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.shadows;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import com.facebook.infer.annotation.Nullsafe;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowDrawable;

/**
 * Shadows a {@link ColorDrawable} to support of drawing its description on a {@link
 * org.robolectric.shadows.ShadowCanvas}
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@Implements(value = ColorDrawable.class)
public class ColorDrawableShadow extends ShadowDrawable {

  // NULLSAFE_FIXME[Field Not Initialized]
  @RealObject private ColorDrawable mRealColorDrawable;

  @Implementation
  public void draw(Canvas canvas) {
    canvas.drawColor(mRealColorDrawable.getColor());
  }
}
