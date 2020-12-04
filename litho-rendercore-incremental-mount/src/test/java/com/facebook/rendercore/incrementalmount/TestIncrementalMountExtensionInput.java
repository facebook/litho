// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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
