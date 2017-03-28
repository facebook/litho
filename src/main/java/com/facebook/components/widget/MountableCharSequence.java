/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.graphics.drawable.Drawable;

/**
 * Mountable {@link CharSequence} that is aware when the Text component using this CharSequence
 * is mounted and unmounted.
 *
 * @see TextSpec
 */
public interface MountableCharSequence extends CharSequence {

  /**
   * This will be called once the text component using this MountableCharSequence is mounted.
   *
   * @param parent the parent drawable the char sequence is bound to
   */
  void onMount(Drawable parent);

  /**
   * This will be called when the text component using MountableCharSequence is unmounted.
   *
   * @param parent the parent drawable the CharSequence was mounted to
   */
  void onUnmount(Drawable parent);
}
