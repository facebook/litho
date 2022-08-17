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

import androidx.annotation.GuardedBy;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that handles generation of unique IDs for RenderUnits for a given ComponentTree. The ID
 * generation uses the component key to create an ID, and creates unique IDs for any components
 * under the same ComponentTree.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class RenderUnitIdGenerator {
  private final AtomicInteger mNextId = new AtomicInteger(1);
  private final int mComponentTreeId;

  @GuardedBy("this")
  private final HashMap<String, Integer> mKeyToId = new HashMap<>();

  public RenderUnitIdGenerator(int componentTreeId) {
    mComponentTreeId = componentTreeId;
  }

  /** Returns the ComponentTree ID that this ID-generator is linked with. */
  public int getComponentTreeId() {
    return mComponentTreeId;
  }

  /**
   * Calculates a returns a unique ID for a given component key and output type. The IDs will be
   * unique for components in the ComponentTree this ID generator is linked with. If an ID was
   * already generated for a given component, the same ID will be returned. Otherwise, a new unique
   * ID will be generated
   *
   * @param componentKey The component key
   * @param type The output type @see OutputUnitType
   */
  public long calculateLayoutOutputId(final String componentKey, final @OutputUnitType int type) {
    return addTypeAndComponentTreeToId(getId(componentKey), type, mComponentTreeId);
  }

  private synchronized int getId(String key) {
    final Integer currentId = mKeyToId.get(key);
    if (currentId != null) {
      return currentId;
    }

    int nextId = mNextId.getAndIncrement();
    mKeyToId.put(key, nextId);
    return nextId;
  }

  private static long addTypeAndComponentTreeToId(
      final int id, final @OutputUnitType int type, final int componentTreeId) {
    return (long) id | ((long) type) << 32 | ((long) componentTreeId) << 35;
  }
}
