// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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
