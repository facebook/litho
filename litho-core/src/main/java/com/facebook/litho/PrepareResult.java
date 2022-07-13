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

package com.facebook.litho;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.Mountable;
import java.util.List;

/**
 * The result of a {@link MountableComponent#prepare} call. This will be the Mountable this
 * component rendered to, potentially as well as other non-Mountable metadata that resulted from
 * that call, such as transitions that should be applied.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class PrepareResult {

  public final @Nullable Mountable<?> mountable;
  public final @Nullable List<Transition> transitions;
  public final @Nullable List<Attachable> useEffectEntries;

  public PrepareResult(
      @Nullable Mountable<?> mountable,
      @Nullable List<Transition> transitions,
      @Nullable List<Attachable> useEffectEntries) {
    this.mountable = mountable;
    this.transitions = transitions;
    this.useEffectEntries = useEffectEntries;
  }
}
