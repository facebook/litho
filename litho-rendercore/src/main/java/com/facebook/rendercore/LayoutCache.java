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

package com.facebook.rendercore;

import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * A Cache that can be used to re use LayoutResults or parts of them across layout calculations.
 * It's responsibility of the implementer of the Layout function to put values in the cache for a
 * given node. Values put in the LayoutCache will only be available in the next layout pass.
 */
public class LayoutCache {
  private final Map<Node, Node.LayoutResult> mWriteCache;
  private final Map<Node, Node.LayoutResult> mReadCache;

  LayoutCache() {
    this(new HashMap<Node, Node.LayoutResult>());
  }

  LayoutCache(@Nullable Map<Node, Node.LayoutResult> cacheResult) {
    mWriteCache = new HashMap<>();
    if (cacheResult == null) {
      mReadCache = new HashMap<>();
    } else {
      mReadCache = cacheResult;
    }
  }

  public void put(Node node, Node.LayoutResult layout) {
    mWriteCache.put(node, layout);
  }

  public Node.LayoutResult get(Node node) {
    return mReadCache.get(node);
  }

  Map<Node, Node.LayoutResult> getWriteCache() {
    return mWriteCache;
  }
}
