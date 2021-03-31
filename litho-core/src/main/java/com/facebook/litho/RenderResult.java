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

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import java.util.List;

/**
 * The result of a {@link Component#render} call. This will be the Component this component rendered
 * to, potentially as well as other non-Component metadata that resulted from that call, such as
 * transitions that should be applied.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
class RenderResult {

  public final @Nullable Component component;
  public final @Nullable List<Transition> transitions;
  public final @Nullable List<Attachable> useEffectEntries;

  public RenderResult(@Nullable Component component) {
    this.component = component;
    this.transitions = null;
    this.useEffectEntries = null;
  }

  public RenderResult(
      @Nullable Component component,
      @Nullable List<Transition> transitions,
      @Nullable List<Attachable> useEffectEntries) {
    this.component = component;
    this.transitions = transitions;
    this.useEffectEntries = useEffectEntries;
  }
}
