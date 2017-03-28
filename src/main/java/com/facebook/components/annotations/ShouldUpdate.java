/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface ShouldUpdate {

  /**
   * This should only be set in the context of MountSpec. Will be ignored for LayoutSpec types.
   * If this is true and this MountSpec is pureRender the mount process will check
   * shouldComponentUpdate before unmounting/mounting in place and only update the content if
   * necessary. If this is false instead, the mount process will only rely on the information
   * provided by the layout process.
   * As a rule of thumb this should only be set to true when for a Component the cost of calling
   * Mount/Unmount greatly exceeds the cost of calling ShouldUpdate.
   */
  boolean onMount() default false;
}
