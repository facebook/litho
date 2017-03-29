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

