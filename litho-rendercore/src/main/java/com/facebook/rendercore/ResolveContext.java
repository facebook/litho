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

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ResolveContext<RenderContext> {

  private final @Nullable RenderContext mRenderContext;
  private final StateUpdateReceiver mStateUpdateReceiver;

  public ResolveContext(
      @Nullable RenderContext renderContext, StateUpdateReceiver stateUpdateReceiver) {
    mRenderContext = renderContext;
    mStateUpdateReceiver = stateUpdateReceiver;
  }

  public @Nullable RenderContext getRenderContext() {
    return mRenderContext;
  }

  public StateUpdateReceiver getStateUpdateReceiver() {
    return mStateUpdateReceiver;
  }
}
