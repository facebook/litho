/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.view.View;
import com.facebook.litho.annotations.Event;

/**
 * Event triggered when focus changes on a Component.
 */
@Event
public class FocusChangedEvent {
  public View view;
  public boolean hasFocus;
}
