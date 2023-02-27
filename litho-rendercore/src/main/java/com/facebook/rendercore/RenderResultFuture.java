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
import com.facebook.rendercore.extensions.RenderCoreExtension;
import java.util.concurrent.Callable;

public class RenderResultFuture<State, RenderContext>
    extends ThreadInheritingPriorityFuture<RenderResult<State, RenderContext>> {

  private final int mSetRootId;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final @Nullable RenderResult<State, RenderContext> mPreviousResult;

  public RenderResultFuture(
      final @Nullable RenderResult<State, RenderContext> previousResult,
      final int setRootId,
      final int widthSpec,
      final int heightSpec,
      final Callable<RenderResult<State, RenderContext>> callable) {
    super(callable, "RenderResultFuture");

    mPreviousResult = previousResult;
    mSetRootId = setRootId;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
  }

  public RenderResultFuture(
      final Context context,
      final RenderState.ResolveFunc<State, RenderContext> resolveFunc,
      final @Nullable RenderContext renderContext,
      final @Nullable RenderCoreExtension<?, ?>[] extensions,
      final @Nullable RenderResult<State, RenderContext> previousResult,
      final int setRootId,
      final int widthSpec,
      final int heightSpec) {
    this(
        previousResult,
        setRootId,
        widthSpec,
        heightSpec,
        () ->
            RenderResult.render(
                context,
                resolveFunc,
                renderContext,
                extensions,
                previousResult,
                setRootId,
                widthSpec,
                heightSpec));
  }

  @Nullable
  public RenderResult<State, RenderContext> getLatestAvailableRenderResult() {
    return isDone() ? runAndGet() : mPreviousResult;
  }

  public int getSetRootId() {
    return mSetRootId;
  }

  public int getWidthSpec() {
    return mWidthSpec;
  }

  public int getHeightSpec() {
    return mHeightSpec;
  }
}
