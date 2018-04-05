/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.viewcompat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Creates a View of the specified type.
 *
 * @param <V> the type of View to create.
 */
public interface ViewCreator<V extends View> {

  /**
   * @param c android Context.
   * @param parent the parent {@link ViewGroup}, or {@code null} if there isn't one.
   * @return a new view of type V.
   */
  V createView(Context c, ViewGroup parent);
}
