/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import com.facebook.litho.annotations.Event;

/**
 * An event that a {@link RecyclerBinder} can trigger to notify the {@link Recycler} that it should
 * re-measure.
 */
@Event
public class ReMeasureEvent {

}
