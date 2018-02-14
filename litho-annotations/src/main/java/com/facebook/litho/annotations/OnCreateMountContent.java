/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The method annotated with this annotation will be called to instantiate the mount content for the
 * {@link MountSpec}. The onCreateMountContent method can only take a
 * com.facebook.litho.ComponentContext as parameter. No props are allowed here.
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnCreateMountContent {

  /**
   * The type of class used for the mount content. During normal compilation, it should never be
   * necessary to specify this explicitly. However, projects that use source-only ABI generation may
   * need to if the mounting type cannot be inferred from the return type.
   */
  MountingType mountingType() default MountingType.INFER;
}
