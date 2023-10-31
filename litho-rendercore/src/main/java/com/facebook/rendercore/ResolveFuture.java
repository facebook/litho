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

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.StateUpdateReceiver.StateUpdate;
import java.util.List;
import java.util.concurrent.Callable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ResolveFuture<State, RenderContext, StateUpdateType extends StateUpdate>
    extends ThreadInheritingPriorityFuture<ResolveResult<Node<RenderContext>, State>> {

  private final int mVersion;
  private final List<StateUpdateType> mStateUpdatesToApply;

  public ResolveFuture(
      final RenderState.ResolveFunc<State, RenderContext, StateUpdateType> resolveFunc,
      ResolveContext<RenderContext, StateUpdateType> resolveContext,
      @Nullable Node<RenderContext> committedTree,
      @Nullable State committedState,
      List<StateUpdateType> stateUpdatesToApply,
      int resolveVersion) {
    super(
        new Callable<ResolveResult<Node<RenderContext>, State>>() {
          @Override
          public ResolveResult<Node<RenderContext>, State> call() {
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

  public List<StateUpdateType> getStateUpdatesToApply() {
    return mStateUpdatesToApply;
  }
}
