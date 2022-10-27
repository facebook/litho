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

public class LithoResolutionResult implements PotentiallyPartialResult {
  public final @Nullable LithoNode node;
  public final MeasuredResultCache cache;
  public final TreeState treeState;
  public final boolean isPartialResult;
  public final int resolveVersion;

  public LithoResolutionResult(
      final @Nullable LithoNode node,
      final MeasuredResultCache cache,
      final TreeState treeState,
      final boolean isPartial,
      final int resolveVersion) {
    this.node = node;
    this.cache = cache;
    this.treeState = treeState;
    this.isPartialResult = isPartial;
    this.resolveVersion = resolveVersion;
  }

  @Override
  public boolean isPartialResult() {
    return isPartialResult;
  }
}
