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
 * Set of events and event params logged by the framework if a {@link ComponentsLogger} was
 * supplied to the {@link ComponentContext} used to create the tree.
 */
public interface FrameworkLogEvents {
  int EVENT_CREATE_LAYOUT = 0;
  int EVENT_CSS_LAYOUT = 1;
  int EVENT_COLLECT_RESULTS = 2;
  int EVENT_LAYOUT_CALCULATE = 3;
  int EVENT_PREPARE_PART_DEFINITION = 4;
  int EVENT_PREPARE_MOUNT = 5;
  int EVENT_MOUNT = 6;
  int EVENT_SHOULD_UPDATE_REFERENCE_LAYOUT_MISMATCH = 7;
  int EVENT_PRE_ALLOCATE_MOUNT_CONTENT = 8;
  int EVENT_ERROR = 9;
  int EVENT_WARNING = 10;
  int EVENT_SECTIONS_CREATE_NEW_TREE = 11;
  int EVENT_SECTIONS_DATA_DIFF_CALCULATE_DIFF = 12;
  int EVENT_SECTIONS_GENERATE_CHANGESET = 13;
  int EVENT_SECTIONS_ON_CREATE_CHILDREN = 14;
  int EVENT_SECTIONS_SET_ROOT = 15;

  String PARAM_COMPONENT = "component";
  String PARAM_LOG_TAG = "log_tag";
  String PARAM_TREE_DIFF_ENABLED = "tree_diff_enabled";
  String PARAM_IS_ASYNC_PREPARE = "is_async_prepare";
  String PARAM_IS_BACKGROUND_LAYOUT = "is_background_layout";
  String PARAM_IS_BACKGROUND_LAYOUT_ENABLED = "is_background_layout_enabled";
  String PARAM_UNMOUNTED_COUNT = "unmounted_count";
  String PARAM_MOVED_COUNT = "moved_count";
  String PARAM_UNCHANGED_COUNT = "unchanged_count";
  String PARAM_MOUNTED_COUNT = "mounted_count";
  String PARAM_UPDATED_COUNT = "updated_count";
  String PARAM_NO_OP_COUNT = "no_op_count";
  String PARAM_IS_DIRTY = "is_dirty";
  String PARAM_MESSAGE = "message";
}
