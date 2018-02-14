/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.annotations;

public enum MountingType {
  /**
   * Mounting type will be inferred from return type. This is the default and is almost always safe
   * during normal compilation.
   */
  INFER,

  /** Mounting type extends View. */
  VIEW,

  /** Mounting type extends Drawable. */
  DRAWABLE,
  ;
}
