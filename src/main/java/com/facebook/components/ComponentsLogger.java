/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

/**
 * An interface for logging life-cycle events in components.
 */
public interface ComponentsLogger {

  int EVENT_CREATE_LAYOUT = 0;
  int EVENT_CSS_LAYOUT = 1;
  int EVENT_COLLECT_RESULTS = 2;
  int EVENT_LAYOUT_CALCULATE = 3;
  int EVENT_PREPARE_PART_DEFINITION = 4;
  int EVENT_PREPARE_MOUNT = 5;
  int EVENT_MOUNT = 6;
  int EVENT_SHOULD_UPDATE_REFERENCE_LAYOUT_MISMATCH = 8;
  int EVENT_PRE_ALLOCATE_MOUNT_CONTENT = 9;
  int EVENT_STETHO_UPDATE_COMPONENT = 10;
  int EVENT_STETHO_INSPECT_COMPONENT = 11;
