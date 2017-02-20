// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

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
