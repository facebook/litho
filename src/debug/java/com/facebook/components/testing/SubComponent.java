/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.testing;

import com.facebook.components.Component;
import com.facebook.components.ComponentLifecycle;

/**
 * Allows convenient type matching comparison for instances of {@link ComponentLifecycle}s.
 * Useful for verifying the existence of sub-components that are part of a layout.
 */
public class SubComponent {

  public static SubComponent of(Class<? extends ComponentLifecycle> componentType) {
