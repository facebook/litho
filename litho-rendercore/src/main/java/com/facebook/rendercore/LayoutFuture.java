// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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

  private final int mVersionId;
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
    mVersionId = layoutVersion;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mTree = node;
  }

  public int getVersion() {
    return mVersionId;
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
