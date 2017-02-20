// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

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

  int ACTION_SUCCESS = 1 << 4;

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

  void eventStart(int eventId, Object object);
  void eventStart(int eventId, Object object, String key, String value);
  void eventEnd(int eventId, Object object, int actionId);
  void eventCancel(int eventId, Object object);
  void eventAddParam(int eventId, Object object, String key, String value);
  void eventAddTag(int eventId, Object object, String tag);

  class LayoutOutputLog {

    long currentId = -1;
    String currentLifecycle;
    int currentIndex = -1;
    int currentLastDuplicatedIdIndex = -1;

    long nextId = -1;
    String nextLifecycle;
    int nextIndex = -1;
    int nextLastDuplicatedIdIndex = -1;

    @Override
    public String toString() {
      return "id: [" + currentId + " - " + nextId + "], "
          + "lifecycle: [" + currentLifecycle + " - " + nextLifecycle + "], "
          + "index: [" + currentIndex + " - " + nextIndex + "], "
          + "lastDuplicatedIdIndex: [" + currentLastDuplicatedIdIndex +
          " - " + nextLastDuplicatedIdIndex + "]";
    }
  }
}
