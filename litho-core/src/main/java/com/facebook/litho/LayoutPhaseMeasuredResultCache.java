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
import java.util.Map;

/**
 * Read-only layout result cache to be used during measure phase. This cache can only be accessed
 * via LithoNode keys.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutPhaseMeasuredResultCache {
  private final Map<LithoNode, LithoLayoutResult> mNodeToResultCache;

  LayoutPhaseMeasuredResultCache(final Map<LithoNode, LithoLayoutResult> nodeToResultCache) {
    mNodeToResultCache = nodeToResultCache;
  }

  /** Return true if there exists a cached layout result for the given node. */
  public boolean hasCachedResult(final LithoNode node) {
    return mNodeToResultCache.containsKey(node);
  }

  /** Returns the cached layout result for the given node, or null if it does not exist. */
  @Nullable
  public LithoLayoutResult getCachedResult(final LithoNode node) {
    return mNodeToResultCache.get(node);
  }
}
