// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.util.Pair;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import java.util.List;
import java.util.concurrent.Callable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ResolveFuture<State, RenderContext>
    extends ThreadInheritingPriorityFuture<Pair<Node<RenderContext>, State>> {

  private final int mVersion;

  public ResolveFuture(
      final RenderState.ResolveFunc<State, RenderContext> resolveFunc,
      ResolveContext resolveContext,
      @Nullable Node<RenderContext> committedTre,
      @Nullable State committedState,
      List<StateUpdateReceiver.StateUpdate<State>> stateUpdatesToApply,
      int version) {
    super(
        new Callable<Pair<Node<RenderContext>, State>>() {
          @Override
          public Pair<Node<RenderContext>, State> call() throws Exception {
            return resolveFunc.resolve(
                resolveContext, committedTre, committedState, stateUpdatesToApply);
          }
        },
        "ResolveFuture");
    mVersion = version;
  }

  public int getVersion() {
    return mVersion;
  }
}
