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

package com.facebook.rendercore;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.List;

class TestRenderUnit extends RenderUnit<View> implements ContentAllocator<View> {

  public TestRenderUnit() {
    super(RenderType.VIEW);
  }

  public TestRenderUnit(List<DelegateBinder<?, ? super View, ?>> fixedMountBinders) {
    super(RenderType.VIEW, fixedMountBinders, Collections.emptyList(), Collections.emptyList());
  }

  public TestRenderUnit(
      List<DelegateBinder<?, ? super View, ?>> fixedMountBinders,
      List<DelegateBinder<?, ? super View, ?>> optionalMountBinders,
      List<DelegateBinder<?, ? super View, ?>> attachBinder) {
    super(RenderType.VIEW, fixedMountBinders, optionalMountBinders, attachBinder);
  }

  public TestRenderUnit(@Nullable final SparseArray<Object> extras) {
    super(
        RenderType.VIEW,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        extras);
  }

  @Override
  public View createContent(Context c) {
    return new View(c);
  }

  @Override
  public ContentAllocator<View> getContentAllocator() {
    return this;
  }

  @Override
  public long getId() {
    return 0;
  }
}
