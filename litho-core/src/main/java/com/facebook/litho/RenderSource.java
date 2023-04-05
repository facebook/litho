// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho;

import androidx.annotation.IntDef;

@IntDef({
  RenderSource.TEST,
  RenderSource.NONE,
  RenderSource.SET_ROOT_SYNC,
  RenderSource.SET_ROOT_ASYNC,
  RenderSource.SET_SIZE_SPEC_SYNC,
  RenderSource.SET_SIZE_SPEC_ASYNC,
  RenderSource.UPDATE_STATE_SYNC,
  RenderSource.UPDATE_STATE_ASYNC,
  RenderSource.MEASURE_SET_SIZE_SPEC,
  RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC,
  RenderSource.RELOAD_PREVIOUS_STATE,
})
public @interface RenderSource {
  int TEST = -2;
  int NONE = -1;
  int SET_ROOT_SYNC = 0;
  int SET_ROOT_ASYNC = 1;
  int SET_SIZE_SPEC_SYNC = 2;
  int SET_SIZE_SPEC_ASYNC = 3;
  int UPDATE_STATE_SYNC = 4;
  int UPDATE_STATE_ASYNC = 5;
  int MEASURE_SET_SIZE_SPEC = 6;
  int MEASURE_SET_SIZE_SPEC_ASYNC = 7;
  int RELOAD_PREVIOUS_STATE = 8;
}
