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

    return pools;
  }
}
