/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.yoga.YogaDirection;

/**
 * Represents a {@link Component}'s computed layout state. The computed bounds will be used by the
 * framework to define the size and position of the component's mounted {@link android.view.View}s
 * and {@link android.graphics.drawable.Drawable}s returned. by {@link
 * Component#mount(ComponentContext, Object)}.
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

  @Nullable
  Drawable getBackground();

  YogaDirection getResolvedLayoutDirection();
}
