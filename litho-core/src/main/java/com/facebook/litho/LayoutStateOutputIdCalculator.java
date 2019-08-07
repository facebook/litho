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

import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;

/**
 * Utility class used to calculate the id of a {@link LayoutOutput} in the context of a
 * {@link LayoutState}. It keeps track of all the {@link LayoutOutput}s with the same baseId
 * in order to generate unique ids even if the baseId is shared by multiple LayoutOutputs.
 */
class LayoutStateOutputIdCalculator {

  @Nullable private LongSparseArray<Integer> mLayoutCurrentSequenceForBaseId;
  @Nullable private LongSparseArray<Integer> mVisibilityCurrentSequenceForBaseId;

  private static final int MAX_SEQUENCE = 65535; // (2^16 - 1)
  private static final int MAX_LEVEL = 255; // (2^8 - 1)

  // 16 bits are for sequence, 2 for type and 8 for level.
  private static final int COMPONENT_ID_SHIFT = 26;
  // 16 bits are sequence and then 2 for type.
  private static final int LEVEL_SHIFT = 18;
  // Last 16 bits are for sequence.
  private static final int TYPE_SHIFT = 16;

  public LayoutStateOutputIdCalculator() {
  }

  void calculateAndSetLayoutOutputIdAndUpdateState(
      LayoutOutput layoutOutput,
      int level,
      @OutputUnitType int type,
      long previousId,
      boolean isCachedOutputUpdated) {

    if (mLayoutCurrentSequenceForBaseId == null) {
      mLayoutCurrentSequenceForBaseId = new LongSparseArray<>(2);
    }

    // We need to assign an id to this LayoutOutput. We want the ids to be as consistent as possible
    // between different layout calculations. For this reason the id generation is a function based
    // on the component of the LayoutOutput, the output type {@link LayoutOutput#LayoutOutputType}
    // the depth of this output in the view hierarchy and an incremental sequence number for
    // LayoutOutputs that have all the other parameters in common.
    long baseLayoutId = LayoutStateOutputIdCalculator.calculateLayoutOutputBaseId(
        layoutOutput,
        level,
        type);
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
      layoutOutputUpdateState = isCachedOutputUpdated ?
          LayoutOutput.STATE_UPDATED :
          LayoutOutput.STATE_DIRTY;
    }
    layoutOutput.setUpdateState(layoutOutputUpdateState);

    long layoutOutputId = LayoutStateOutputIdCalculator.calculateId(baseLayoutId, sequence);
    layoutOutput.setId(layoutOutputId);

    mLayoutCurrentSequenceForBaseId.put(baseLayoutId, sequence + 1);
  }

  void calculateAndSetVisibilityOutputId(
      VisibilityOutput visibilityOutput,
      int level,
      long previousId) {

    if (mVisibilityCurrentSequenceForBaseId == null) {
      mVisibilityCurrentSequenceForBaseId = new LongSparseArray<>(2);
    }

    // We need to assign an id to this VisibilityOutput. We want the ids to be as consistent as
    // possible between different layout calculations. For this reason the id generation is a
    // function based on the component of the VisibilityOutput, the depth of this output in the view
    // hierarchy and an incremental sequence number for VisibilityOutputs that have all the other
    // parameters in common.
    final long baseVisibilityId = calculateVisibilityOutputBaseId(
        visibilityOutput,
        level);
    int sequence;
    if (previousId > 0 && getLevelFromId(previousId) == level) {
      sequence = getSequenceFromId(previousId);
    } else {
      sequence = -1;
    }

    final int currentSequence = mVisibilityCurrentSequenceForBaseId.get(baseVisibilityId, 0);
    // If sequence is positive we are trying to re-use the id from a previous VisibilityOutput.
    // We can only do that if the sequence that visibilityOutput used is not already assigned.
    if (sequence < currentSequence) {
      sequence = currentSequence + 1;
    }

    final long visibilityOutputId = calculateId(baseVisibilityId, sequence);
    visibilityOutput.setId(visibilityOutputId);

    mVisibilityCurrentSequenceForBaseId.put(baseVisibilityId, sequence + 1);
  }

  void clear() {
    if (mLayoutCurrentSequenceForBaseId != null) {
      mLayoutCurrentSequenceForBaseId.clear();
    }
    if (mVisibilityCurrentSequenceForBaseId != null) {
      mVisibilityCurrentSequenceForBaseId.clear();
    }
  }

  /**
   * Calculates the final id for a LayoutOutput based on the baseId see
   * {@link LayoutStateOutputIdCalculator#calculateLayoutOutputBaseId(LayoutOutput, int, int)} and
   * on a sequence number. The sequence number must be guaranteed to be unique for LayoutOutputs
   * with the same baseId.
   */
  static long calculateId(long baseId, int sequence) {
    if (sequence < 0 || sequence > MAX_SEQUENCE) {
      throw new IllegalArgumentException("Sequence must be non-negative and no greater than " +
          MAX_SEQUENCE + " actual sequence "+sequence);
    }

    return baseId | sequence;
  }

  /**
   * Calculates an id for a {@link LayoutOutput}. See {@link
   * LayoutStateOutputIdCalculator#calculateLayoutOutputBaseId(LayoutOutput, int, int)} and {@link
   * LayoutStateOutputIdCalculator#calculateId(long, int)}.
   */
  static long calculateLayoutOutputId(
      LayoutOutput layoutOutput, int level, @OutputUnitType int type, int sequence) {
    long baseId = calculateLayoutOutputBaseId(layoutOutput, level, type);
    return calculateId(baseId, sequence);
  }

  /**
   * Calculates an id for a {@link VisibilityOutput}. See
   * {@link LayoutStateOutputIdCalculator#calculateVisibilityOutputBaseId(VisibilityOutput, int)}
   * and {@link LayoutStateOutputIdCalculator#calculateId(long, int)}.
   */
  static long calculateVisibilityOutputId(
      VisibilityOutput visibilityOutput,
      int level,
      int sequence) {
    final long baseId = calculateVisibilityOutputBaseId(visibilityOutput, level);
    return calculateId(baseId, sequence);
  }

  /**
   * @return the sequence part of an id.
   */
  static int getSequenceFromId(long id) {
    return (int) id & 0x00FFFF;
  }

  /**
   * @return the level part of an id.
   */
  static int getLevelFromId(long id) {
    return (int) ((id >> LEVEL_SHIFT) & 0xFF);
  }

  /** @return the type part of an id. */
  static @OutputUnitType int getTypeFromId(long id) {
    if (id == MountState.ROOT_HOST_ID) {
      // special case
      return OutputUnitType.HOST;
    }
    return (int) ((id >> TYPE_SHIFT) & 0x3);
  }

  /**
   * Calculates a base id for an {@link LayoutOutput} based on the {@link Component}, the depth in
   * the View hierarchy, and the type of output see {@link OutputUnitType}.
   */
  private static long calculateLayoutOutputBaseId(
      LayoutOutput layoutOutput, int level, @OutputUnitType int type) {
    if (level < 0 || level > MAX_LEVEL) {
      throw new IllegalArgumentException(
          "Level must be non-negative and no greater than " + MAX_LEVEL + " actual level " + level);
    }

    long componentId =
        layoutOutput.getComponent() != null ? layoutOutput.getComponent().getTypeId() : 0L;

    long componentShifted = componentId << COMPONENT_ID_SHIFT;
    long levelShifted = ((long) level) << LEVEL_SHIFT;
    long typeShifted = ((long) type) << TYPE_SHIFT;

    return 0L | componentShifted | levelShifted | typeShifted;
  }

  /**
   * Calculates a base id for a {@link VisibilityOutput} based on the {@link Component} and the
   * depth in the View hierarchy.
   */
  private static long calculateVisibilityOutputBaseId(
      VisibilityOutput visibilityOutput,
      int level) {
    if (level < 0 || level > MAX_LEVEL) {
      throw new IllegalArgumentException(
          "Level must be non-negative and no greater than " + MAX_LEVEL + " actual level " + level);
    }

    final long componentId =
        visibilityOutput.getComponent() != null ? visibilityOutput.getComponent().getTypeId() : 0L;

    final long componentShifted = componentId << COMPONENT_ID_SHIFT;
    final long levelShifted = ((long) level) << LEVEL_SHIFT;

    return 0L | componentShifted | levelShifted;
  }
}
