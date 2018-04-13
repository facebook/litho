/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of methods which expose internals of the framework. These APIs should not be considered
 * public and should never be used in production. They are however useful when debugging and
 * building debugging tools.
 */
public final class LithoDebugInfo {

  private LithoDebugInfo() {}

  /** @return a list of active recycling pools used within Litho. */
  public static List<PoolWithDebugInfo> getPools() {
    List<PoolWithDebugInfo> pools = new ArrayList<>();

    pools.addAll(ComponentsPools.getMountContentPools());
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
    pools.add(ComponentsPools.sRectFPool);
    pools.add(ComponentsPools.sRectPool);
    pools.add(ComponentsPools.sEdgesPool);
    pools.add(ComponentsPools.sTransitionContextPool);
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
