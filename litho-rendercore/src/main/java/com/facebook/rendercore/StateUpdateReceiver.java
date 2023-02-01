// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface StateUpdateReceiver<State> {
  interface StateUpdate<State> {
    State update(State state);
  }

  void enqueueStateUpdate(StateUpdate<State> stateUpdate);
}
