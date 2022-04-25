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

package com.facebook.litho.widget;

import android.view.View;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.infer.annotation.OkToExtend;
import com.facebook.litho.LithoView;

/** The ViewHolder that hosts LithoView used by {@link RecyclerBinder} */
@OkToExtend
public abstract class RecyclerBinderViewHolder extends RecyclerView.ViewHolder {

  public RecyclerBinderViewHolder(View itemView) {
    super(itemView);
  }

  /** Return the LithoView, which will be used to bind Components. */
  @Nullable
  public abstract LithoView getLithoView();

  /**
   * Overwrite the LayoutPrams of the LithoView, which is an optional step if there is no need to do
   * so.
   */
  public abstract void setLithoViewLayoutParams(
      LithoView lithoView,
      int width,
      int height,
      int widthSpec,
      int heightSpec,
      boolean isFullSpan);
}
