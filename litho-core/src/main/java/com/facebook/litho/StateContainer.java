/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import com.facebook.infer.annotation.Nullsafe;

/**
 * Implemented by the class used to store state within both Components and Sections to store state.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class StateContainer {
  public abstract void applyStateUpdate(StateUpdate stateUpdate);

  public static final class StateUpdate {
    public final int type;
    public final Object[] params;

    public StateUpdate(int type, Object... params) {
      this.type = type;
      this.params = params;
    }
  }
}
