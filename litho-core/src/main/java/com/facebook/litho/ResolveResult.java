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

import android.util.Pair;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ResolveResult implements PotentiallyPartialResult {
  public final @Nullable LithoNode node;
  public final ComponentContext context;
  public final Component component;
  private final AtomicReference<MeasuredResultCache> cache;
  public final TreeState treeState;
  public final boolean isPartialResult;
  public final int version;
  public final @Nullable List<Pair<String, EventHandler<?>>> createdEventHandlers;
  public final @Nullable List<Attachable> attachables;
  public final @Nullable ResolveStateContext contextForResuming;

  public ResolveResult(
      final @Nullable LithoNode node,
      final ComponentContext context,
      final Component component,
      final MeasuredResultCache cache,
      final TreeState treeState,
      final boolean isPartial,
      final int version,
      final @Nullable List<Pair<String, EventHandler<?>>> createdEventHandlers,
      final @Nullable List<Attachable> attachables,
      final @Nullable ResolveStateContext contextForResuming) {
    this.node = node;
    this.context = context;
    this.component = component;
    this.cache = new AtomicReference<>(cache);
    this.treeState = treeState;
    this.isPartialResult = isPartial;
    this.version = version;
    this.createdEventHandlers = createdEventHandlers;
    this.attachables = attachables;
    this.contextForResuming = contextForResuming;
  }

  @Override
  public boolean isPartialResult() {
    return isPartialResult;
  }

  public MeasuredResultCache consumeCache() {
    return cache.getAndSet(MeasuredResultCache.EMPTY);
  }
}
