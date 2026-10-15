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

package com.facebook.rendercore.incrementalmount

class TestIncrementalMountExtensionInput(vararg incrementalMountOutputs: IncrementalMountOutput) :
    IncrementalMountExtensionInput {

  val incrementalMountOutputMap: LinkedHashMap<Long, IncrementalMountOutput> = LinkedHashMap()
  private val tops: MutableList<IncrementalMountOutput> = ArrayList()
  private val bottoms: MutableList<IncrementalMountOutput> = ArrayList()

  init {
    for (output in incrementalMountOutputs) {
      incrementalMountOutputMap[output.id] = output
      tops.add(output)
      bottoms.add(output)
    }

    tops.sortWith(IncrementalMountRenderCoreExtension.TopsComparator)
    bottoms.sortWith(IncrementalMountRenderCoreExtension.BottomsComparator)
  }

  override fun getOutputsOrderedByTopBounds(): List<IncrementalMountOutput> = tops

  override fun getOutputsOrderedByBottomBounds(): List<IncrementalMountOutput> = bottoms

  override fun getIncrementalMountOutputForId(id: Long): IncrementalMountOutput? =
      incrementalMountOutputMap[id]

  override fun getIncrementalMountOutputs(): Collection<IncrementalMountOutput> =
      incrementalMountOutputMap.values

  override fun getIncrementalMountOutputCount(): Int = incrementalMountOutputMap.size

  override fun renderUnitWithIdHostsRenderTrees(id: Long): Boolean = true
}
