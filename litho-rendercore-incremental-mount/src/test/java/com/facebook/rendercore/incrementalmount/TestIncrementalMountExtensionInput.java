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

package com.facebook.rendercore.incrementalmount;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestIncrementalMountExtensionInput implements IncrementalMountExtensionInput {
  final Map<Long, IncrementalMountOutput> mIncrementalMountOutputs = new LinkedHashMap<>();
  final List<IncrementalMountOutput> tops = new ArrayList<>();
  final List<IncrementalMountOutput> bottoms = new ArrayList<>();

  public TestIncrementalMountExtensionInput(IncrementalMountOutput... incrementalMountOutputs) {
    for (IncrementalMountOutput output : incrementalMountOutputs) {
      mIncrementalMountOutputs.put(output.getId(), output);
      tops.add(output);
      bottoms.add(output);
    }

    Collections.sort(tops, IncrementalMountRenderCoreExtension.sTopsComparator);
    Collections.sort(bottoms, IncrementalMountRenderCoreExtension.sBottomsComparator);
  }

  @Override
  public List<IncrementalMountOutput> getOutputsOrderedByTopBounds() {
    return tops;
  }

  @Override
  public List<IncrementalMountOutput> getOutputsOrderedByBottomBounds() {
    return bottoms;
  }

  @Nullable
  @Override
  public IncrementalMountOutput getIncrementalMountOutputForId(long id) {
    return mIncrementalMountOutputs.get(id);
  }

  @Override
  public Collection<IncrementalMountOutput> getIncrementalMountOutputs() {
    return mIncrementalMountOutputs.values();
  }

  @Override
  public int getIncrementalMountOutputCount() {
    return mIncrementalMountOutputs.size();
  }

  @Override
  public boolean renderUnitWithIdHostsRenderTrees(long id) {
    return true;
  }
}
