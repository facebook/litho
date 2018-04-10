/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.text.style.ClickableSpan;
import android.view.View;

/** Extension of {@link ClickableSpan} that provides longclick capability in addition to click. */
public abstract class LongClickableSpan extends ClickableSpan {
  /**
   * Callback for longclick of this span.
   *
   * @return true if the callback consumed the longclick, false otherwise.
   */
  public abstract boolean onLongClick(View view);
}
