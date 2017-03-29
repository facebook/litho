/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

/**
 * An interface for {@link SpecModel}s that can be pure render.
 */
public interface HasPureRender {

  /**
   * Whether this spec is pure render or not. If a spec is pure render then when the
   * ComponentTree for this component is updated, if nothing changes then the measurements for this
   * component can be re-used.
   */
  boolean isPureRender();
}
