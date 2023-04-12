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
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.extensions.RenderCoreExtension;

/**
 * A LayoutContext encapsulates all the data needed during a layout pass. It contains - The Android
 * context associated with this layout calculation. - The version of the layout calculation. - The
 * LayoutCache for this layout calculation. Access to the cache is only valid during the execution
 * of the Node's calculateLayout function.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutContext<RenderContext> {

  private final Context androidContext;
  private final int layoutVersion;
  private @Nullable LayoutCache layoutCache;
  private final @Nullable RenderContext mRenderContext;
  private final @Nullable RenderCoreExtension<?, ?>[] extensions;
  private @Nullable Object previousLayoutData;
  @Nullable private LayoutContextExtraData<?> layoutContextExtraData;

  public LayoutContext(
      final Context androidContext,
      final @Nullable RenderContext renderContext,
      final int layoutVersion,
      final @Nullable LayoutCache layoutCache,
      final @Nullable RenderCoreExtension<?, ?>[] extensions) {
    this.androidContext = androidContext;
    this.layoutVersion = layoutVersion;
    this.layoutCache = layoutCache;
    this.mRenderContext = renderContext;
    this.extensions = extensions;
  }

  public @Nullable RenderContext getRenderContext() {
    return mRenderContext;
  }

  public Context getAndroidContext() {
    return androidContext;
  }

  public int getLayoutVersion() {
    return layoutVersion;
  }

  public LayoutCache getLayoutCache() {
    if (layoutCache == null) {
      throw new IllegalStateException(
          "Trying to access the LayoutCache from outside a layout call");
    }

    return layoutCache;
  }

  void clearCache() {
    layoutCache = null;
  }

  @Nullable
  public RenderCoreExtension<?, ?>[] getExtensions() {
    return extensions;
  }

  public void setPreviousLayoutDataForCurrentNode(@Nullable Object previousLayoutData) {
    this.previousLayoutData = previousLayoutData;
  }

  public @Nullable Object consumePreviousLayoutDataForCurrentNode() {
    final Object data = previousLayoutData;
    previousLayoutData = null;
    return data;
  }

  public void setLayoutContextExtraData(@Nullable LayoutContextExtraData<?> extraData) {
    this.layoutContextExtraData = extraData;
  }

  @Nullable
  public LayoutContextExtraData<?> getLayoutContextExtraData() {
    return layoutContextExtraData;
  }
}
