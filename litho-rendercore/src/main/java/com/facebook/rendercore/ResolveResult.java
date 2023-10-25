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

import androidx.core.util.ObjectsCompat;
import com.facebook.infer.annotation.Nullsafe;
import java.util.List;
import javax.annotation.Nullable;

/** Represents the result of resolving a Rendercore {@link Node} */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ResolveResult<T extends Node<?>, State> {
  public final T resolvedNode;
  @Nullable public final State resolvedState;
  @Nullable public final List<StateUpdateReceiver.StateUpdate<?>> appliedStateUpdates;

  public ResolveResult(
      final T resolvedNode,
      @Nullable final State resolvedState,
      @Nullable List<StateUpdateReceiver.StateUpdate<?>> appliedStateUpdates) {
    this.resolvedNode = resolvedNode;
    this.resolvedState = resolvedState;
    this.appliedStateUpdates = appliedStateUpdates;
  }

  public ResolveResult(final T resolvedNode, @Nullable final State resolvedState) {
    this(resolvedNode, resolvedState, null);
  }

  public ResolveResult(final T resolvedNode) {
    this(resolvedNode, null);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ResolveResult)) {
      return false;
    }

    final ResolveResult that = (ResolveResult) obj;
    return ObjectsCompat.equals(resolvedNode, that.resolvedNode)
        && ObjectsCompat.equals(resolvedState, that.resolvedState)
        && ObjectsCompat.equals(appliedStateUpdates, that.appliedStateUpdates);
  }

  @Override
  public int hashCode() {
    return (resolvedNode.hashCode())
        ^ (resolvedState == null ? 0 : resolvedState.hashCode())
        ^ (appliedStateUpdates == null ? 0 : appliedStateUpdates.hashCode());
  }
}
