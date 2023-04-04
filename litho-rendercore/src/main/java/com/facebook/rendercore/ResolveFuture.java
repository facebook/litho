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

import android.util.Pair;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.StateUpdateReceiver.StateUpdate;
import java.util.List;
import java.util.concurrent.Callable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ResolveFuture<State, RenderContext>
    extends ThreadInheritingPriorityFuture<Pair<Node<RenderContext>, State>> {

  private final int mVersion;
  private final List<StateUpdate> mStateUpdatesToApply;

  public ResolveFuture(
      final RenderState.ResolveFunc<State, RenderContext> resolveFunc,
      ResolveContext<RenderContext> resolveContext,
      @Nullable Node<RenderContext> committedTree,
      @Nullable State committedState,
      List<StateUpdate> stateUpdatesToApply,
      int resolveVersion) {
    super(
        new Callable<Pair<Node<RenderContext>, State>>() {
          @Override
          public Pair<Node<RenderContext>, State> call() {
            return resolveFunc.resolve(
                resolveContext, committedTree, committedState, stateUpdatesToApply);
          }
        },
        "ResolveFuture");
    mVersion = resolveVersion;
    mStateUpdatesToApply = stateUpdatesToApply;
  }

  public int getVersion() {
    return mVersion;
  }

  public List<StateUpdate> getStateUpdatesToApply() {
    return mStateUpdatesToApply;
  }
}
