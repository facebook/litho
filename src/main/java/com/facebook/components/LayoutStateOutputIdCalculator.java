/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.v4.util.LongSparseArray;

/**
 * Utility class used to calculate the id of a {@link LayoutOutput} in the context of a
 * {@link LayoutState}. It keeps track of all the {@link LayoutOutput}s with the same baseId
 * in order to generate unique ids even if the baseId is shared by multiple LayoutOutputs.
 */
class LayoutStateOutputIdCalculator {

  private final LongSparseArray<Integer> mLayoutCurrentSequenceForBaseId = new LongSparseArray<>(8);
  private final LongSparseArray<Integer> mVisibilityCurrentSequenceForBaseId =
      new LongSparseArray<>(8);

  private static final int MAX_SEQUENCE = 65535; // (2^16 - 1)
  private static final int MAX_LEVEL = 255; // (2^8 - 1)

  // 16 bits are for sequence, 2 for type and 8 for level.
  private static final short COMPONENT_ID_SHIFT = 26;
  // 16 bits are sequence and then 2 for type.
  private static final short LEVEL_SHIFT = 18;
  // Last 16 bits are for sequence.
  private static final short TYPE_SHIFT = 16;

  void calculateAndSetLayoutOutputIdAndUpdateState(
      LayoutOutput layoutOutput,
      int level,
      @LayoutOutput.LayoutOutputType int type,
      long previousId,
      boolean isCachedOutputUpdated) {

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
