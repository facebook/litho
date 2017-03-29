/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.annotations.Event;

/**
 * Event triggered when a Component becomes invisible. This is the same with exiting the Visible
 * Range, the Focused Range and the Full Impression Range. All the code that needs to be executed
 * when a component leaves any of these ranges should be written in the handler for this event.
 */
@Event
public class InvisibleEvent {
}
