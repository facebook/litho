// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ResolveContext {
  private final StateUpdateReceiver mStateUpdateReceiver;

  public ResolveContext(StateUpdateReceiver stateUpdateReceiver) {
    mStateUpdateReceiver = stateUpdateReceiver;
  }

  public StateUpdateReceiver getStateUpdateReceiver() {
    return mStateUpdateReceiver;
  }
}
