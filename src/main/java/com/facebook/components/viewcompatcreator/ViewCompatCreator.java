/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.viewcompatcreator;

import android.content.Context;
import android.view.View;

/**
 * Creates a View of the specified type. Used as the mount content for ViewCompatComponent.
 * Views created with the same {@link ViewCompatCreator} will be recycled across instances of
 * ViewCompatComponent.
 * @param <V> the type of View to create.
 */
public interface ViewCompatCreator<V extends View> {

  /**
   * @param c android Context.
   * @return a new view of type V.
   */
  V createView(Context c);
}
