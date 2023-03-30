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

import static com.facebook.rendercore.RenderResult.shouldReuseResult;

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import java.util.Objects;
import java.util.concurrent.Callable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutFuture<State, RenderContext>
    extends ThreadInheritingPriorityFuture<RenderResult<State, RenderContext>> {

  private final int mVersion;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final Node<RenderContext> mTree;

  public LayoutFuture(
      Context context,
      RenderContext renderContext,
      Node<RenderContext> node,
      @Nullable State state,
      int layoutVersion,
      @Nullable RenderResult<State, RenderContext> previousResult,
      @Nullable RenderCoreExtension<?, ?>[] extensions,
      int widthSpec,
      int heightSpec) {
    super(
        new Callable<RenderResult<State, RenderContext>>() {
          @Override
          public RenderResult<State, RenderContext> call() {
            if (shouldReuseResult(node, widthSpec, heightSpec, previousResult)) {
              Objects.requireNonNull(previousResult);
              return new RenderResult<>(
                  previousResult.getRenderTree(), node, previousResult.getLayoutCacheData(), state);
            }
            return RenderResult.layout(
                RenderResult.createLayoutContext(
                    previousResult, renderContext, context, layoutVersion, extensions),
                node,
                state,
                widthSpec,
                heightSpec);
          }
        },
        "LayoutFuture");
    mVersion = layoutVersion;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mTree = node;
  }

  public int getVersion() {
    return mVersion;
  }

  public int getWidthSpec() {
    return mWidthSpec;
  }

  public int getHeightSpec() {
    return mHeightSpec;
  }

  public Node<RenderContext> getTree() {
    return mTree;
  }
}
