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

import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;

/**
 * Utility class used to calculate the id of a {@link LayoutOutput} in the context of a {@link
 * LayoutState}. It keeps track of all the {@link LayoutOutput}s with the same baseId in order to
 * generate unique ids even if the baseId is shared by multiple LayoutOutputs.
 */
class LayoutStateOutputIdCalculator {

  @Nullable private LongSparseArray<Integer> mLayoutCurrentSequenceForBaseId;

  // LayoutOutputID stored in long (64 bits):  1 bit for sign, 63 for ID
  // 36 for component_ID, 8 for level, 3 for type, and 16 for sequence
  // ------------------------------------########...****************
  //            COMPONENT ID             LEVEL   TYP    SEQUENCE

  private static final int SEQUENCE_BITS = 16;
  private static final int TYPE_BITS = 3;
  private static final int LEVEL_BITS = 8;

  private static final int MAX_SEQUENCE = (1 << SEQUENCE_BITS) - 1; // (2^16 - 1)
  private static final int MAX_LEVEL = (1 << LEVEL_BITS) - 1; // (2^8 - 1)

  private static final int TYPE_SHIFT = SEQUENCE_BITS; // 16
  private static final int LEVEL_SHIFT = TYPE_SHIFT + TYPE_BITS; // 19 = 16 + 3
  private static final int COMPONENT_ID_SHIFT = LEVEL_SHIFT + LEVEL_BITS; // 27 = 19 + 8

  private static final int SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;
  private static final int TYPE_MASK = (1 << TYPE_BITS) - 1;
  private static final int LEVEL_MASK = (1 << LEVEL_BITS) - 1;

  public LayoutStateOutputIdCalculator() {}

  void calculateAndSetLayoutOutputIdAndUpdateState(
      Component component,
      LayoutOutput layoutOutput,
      int level,
      @OutputUnitType int type,
      long previousId,
      boolean isCachedOutputUpdated,
      @Nullable DebugHierarchy.Node hierarchy) {

    if (mLayoutCurrentSequenceForBaseId == null) {
      mLayoutCurrentSequenceForBaseId = new LongSparseArray<>(2);
    }

    // We need to assign an id to this LayoutOutput. We want the ids to be as consistent as possible
    // between different layout calculations. For this reason the id generation is a function based
    // on the component of the LayoutOutput, the output type {@link LayoutOutput#LayoutOutputType}
    // the depth of this output in the view hierarchy and an incremental sequence number for
    // LayoutOutputs that have all the other parameters in common.
    long baseLayoutId =
        LayoutStateOutputIdCalculator.calculateLayoutOutputBaseId(component, level, type);
    int sequence;
    if (previousId > 0 && getLevelFromId(previousId) == level) {
      sequence = getSequenceFromId(previousId);
    } else {
      sequence = -1;
    }

    final int currentSequence = mLayoutCurrentSequenceForBaseId.get(baseLayoutId, 0);
    final int layoutOutputUpdateState;
    // If sequence is positive we are trying to re-use the id from a previous LayoutOutput.
    // We can only do that if the sequence that layoutOutput used is not already assigned.
    if (sequence < currentSequence) {
      // If we failed re-using the same id from the previous LayoutOutput we default the
      // UpdateState to STATE_UNKNOWN
      sequence = currentSequence + 1;
      layoutOutputUpdateState = LayoutOutput.STATE_UNKNOWN;
    } else {
      // If we successfully re-used the id from a previous LayoutOutput we can also set the
      // UpdateState so that MountItem won't need to call shouldComponentUpdate.
      layoutOutputUpdateState =
          isCachedOutputUpdated ? LayoutOutput.STATE_UPDATED : LayoutOutput.STATE_DIRTY;
    }
    layoutOutput.setUpdateState(layoutOutputUpdateState);

    long layoutOutputId = LayoutStateOutputIdCalculator.calculateId(baseLayoutId, sequence);
    layoutOutput.setId(layoutOutputId);
    if (hierarchy != null) {
      // Add the type to the debug hierarchy.
      layoutOutput.setHierarchy(hierarchy.mutateType(type));
    }

    mLayoutCurrentSequenceForBaseId.put(baseLayoutId, sequence + 1);
  }

  void clear() {
    if (mLayoutCurrentSequenceForBaseId != null) {
      mLayoutCurrentSequenceForBaseId.clear();
    }
  }

  /**
   * Calculates the final id for a LayoutOutput based on the baseId see {@link
   * LayoutStateOutputIdCalculator#calculateLayoutOutputBaseId(Component, int, int)} and on a
   * sequence number. The sequence number must be guaranteed to be unique for LayoutOutputs with the
   * same baseId.
   */
  static long calculateId(long baseId, int sequence) {
    if (sequence < 0 || sequence > MAX_SEQUENCE) {
      throw new IllegalArgumentException(
          "Sequence must be non-negative and no greater than "
              + MAX_SEQUENCE
              + " actual sequence "
              + sequence);
    }

    return baseId | sequence;
  }

  /**
   * Calculates an id for a {@link LayoutOutput}. See {@link
   * LayoutStateOutputIdCalculator#calculateLayoutOutputBaseId(Component, int, int)} and {@link
   * LayoutStateOutputIdCalculator#calculateId(long, int)}.
   */
  static long calculateLayoutOutputId(
      Component component, int level, @OutputUnitType int type, int sequence) {
    long baseId = calculateLayoutOutputBaseId(component, level, type);
    return calculateId(baseId, sequence);
  }

  /** @return the sequence part of an id. */
  static int getSequenceFromId(long id) {
    return (int) (id & SEQUENCE_MASK);
  }

  /** @return the level part of an id. */
  static int getLevelFromId(long id) {
    return (int) ((id >> LEVEL_SHIFT) & LEVEL_MASK);
  }

  /** @return the type part of an id. */
  static @OutputUnitType int getTypeFromId(long id) {
    if (id == ROOT_HOST_ID) {
      // special case
      return OutputUnitType.HOST;
    }
    return (int) ((id >> TYPE_SHIFT) & TYPE_MASK);
  }

  /**
   * Calculates a base id for an {@link LayoutOutput} based on the {@link Component}, the depth in
   * the View hierarchy, and the type of output see {@link OutputUnitType}.
   */
  private static long calculateLayoutOutputBaseId(
      Component component, int level, @OutputUnitType int type) {
    if (level < 0 || level > MAX_LEVEL) {
      throw new IllegalArgumentException(
          "Level must be non-negative and no greater than " + MAX_LEVEL + " actual level " + level);
    }

    long componentId = component.getTypeId();

    long componentShifted = componentId << COMPONENT_ID_SHIFT;
    long levelShifted = ((long) level) << LEVEL_SHIFT;
    long typeShifted = ((long) type) << TYPE_SHIFT;

    return 0L | componentShifted | levelShifted | typeShifted;
  }
}
