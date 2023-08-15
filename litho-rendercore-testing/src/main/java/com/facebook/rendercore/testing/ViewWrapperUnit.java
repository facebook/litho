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

package com.facebook.rendercore.testing;

import static com.facebook.rendercore.RenderUnit.DelegateBinder.createDelegateBinder;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.RenderUnit;
import java.util.Collections;

public class ViewWrapperUnit extends RenderUnit<View> implements ContentAllocator<View> {

  private final View view;
  private final long id;

  public ViewWrapperUnit(final View view, final long id) {
    this(view, id, null);
  }

  public ViewWrapperUnit(final View view, final long id, @Nullable SparseArray<Object> extras) {
    super(
        RenderType.VIEW,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        extras);
    this.view = view;
    this.id = id;
  }

  @Override
  public View createContent(Context c) {
    return view;
  }

  @Override
  public Class<?> getPoolableContentType() {
    return getRenderContentType();
  }

  @Override
  public ContentAllocator<View> getContentAllocator() {
    return this;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public Class<?> getRenderContentType() {
    return view.getClass();
  }

  @Override
  public boolean isRecyclingDisabled() {
    return true;
  }

  public ViewWrapperUnit addBindBinders(RenderUnit.Binder<ViewWrapperUnit, View, Void>... binders) {
    for (Binder<ViewWrapperUnit, View, Void> binder : binders) {
      super.addAttachBinder(createDelegateBinder(this, binder));
    }

    return this;
  }

  public ViewWrapperUnit addMounBinders(RenderUnit.Binder<ViewWrapperUnit, View, Void>... binders) {
    for (Binder<ViewWrapperUnit, View, Void> binder : binders) {
      super.addOptionalMountBinder(createDelegateBinder(this, binder));
    }

    return this;
  }
}
