/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

/**
 * Describes the lifecycle step a component is currently in. This mirrors the delegate methods
 * available in {@link ComponentLifecycle}.
 */

// TODO(T25269350): Lifecycle phases other than onCreateLayout aren't supported at the moment.
public enum LifecyclePhase {
  ON_CREATE_LAYOUT,
}
