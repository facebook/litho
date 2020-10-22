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

package com.facebook.rendercore.testing;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.facebook.rendercore.RenderUnit;

public class SimpleDrawableUnit extends RenderUnit<Drawable> {

  private final Drawable drawable;
  private final long id;

  public SimpleDrawableUnit(final Drawable drawable, final long id) {
    super(RenderType.DRAWABLE);
    this.drawable = drawable;
    this.id = id;
  }

  @Override
  public Drawable createContent(Context c) {
    return drawable;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public Object getRenderContentType() {
    return drawable.getClass();
  }
}
