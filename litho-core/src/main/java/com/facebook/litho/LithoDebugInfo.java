/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;

/**
 * A set of methods which expose internals of the framework. These APIs should not be considered
 * public and should never be used in production. They are however useful when debugging and
 * building debugging tools.
 */
public class LithoDebugInfo {

  /**
   * @return A list of active recycling pools used within Litho.
   */
  public static List<RecyclePool> getPools() {
    List<RecyclePool> pools = new ArrayList<>();
    
    for (SparseArray<RecyclePool> contentPools :
        ComponentsPools.sMountContentPoolsByContext.values()) {
      for (int i = 0, count = contentPools.size(); i < count; i++) {
        pools.add(contentPools.valueAt(i));
      }
    }

    pools.add(ComponentsPools.sLayoutStatePool);
    pools.add(ComponentsPools.sInternalNodePool);
    pools.add(ComponentsPools.sNodeInfoPool);
    pools.add(ComponentsPools.sViewNodeInfoPool);
    pools.add(ComponentsPools.sYogaNodePool);
    pools.add(ComponentsPools.sMountItemPool);
    pools.add(ComponentsPools.sLayoutOutputPool);
    pools.add(ComponentsPools.sVisibilityOutputPool);
    pools.add(ComponentsPools.sVisibilityItemPool);
    pools.add(ComponentsPools.sOutputPool);
    pools.add(ComponentsPools.sDiffNodePool);
    pools.add(ComponentsPools.sDiffPool);
    pools.add(ComponentsPools.sComponentTreeBuilderPool);
    pools.add(ComponentsPools.sStateHandlerPool);
    pools.add(ComponentsPools.sMountItemScrapArrayPool);
    pools.add(ComponentsPools.sTouchableScrapArrayPool);
    pools.add(ComponentsPools.sRectFPool);
    pools.add(ComponentsPools.sRectPool);
    pools.add(ComponentsPools.sEdgesPool);
    pools.add(ComponentsPools.sTransitionContextPool);
    pools.add(ComponentsPools.sTransitionManagerPool);
    pools.add(ComponentsPools.sDisplayListDrawablePool);
    pools.add(ComponentsPools.sTreePropsMapPool);
    pools.add(ComponentsPools.sArraySetPool);
    pools.add(ComponentsPools.sArrayDequePool);
    pools.add(ComponentsPools.sLogEventPool);

    if (ComponentsPools.sTestOutputPool != null) {
      pools.add(ComponentsPools.sTestOutputPool);
    }

    if (ComponentsPools.sTestItemPool != null) {
      pools.add(ComponentsPools.sTestItemPool);
    }

    if (ComponentsPools.sBorderColorDrawablePool != null) {
      pools.add(ComponentsPools.sBorderColorDrawablePool);
    }

    return pools;
  }
}
