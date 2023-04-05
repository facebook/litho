// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho;

import androidx.annotation.IntDef;

@IntDef({
  CalculateLayoutSource.TEST,
  CalculateLayoutSource.NONE,
  CalculateLayoutSource.SET_ROOT_SYNC,
  CalculateLayoutSource.SET_ROOT_ASYNC,
  CalculateLayoutSource.SET_SIZE_SPEC_SYNC,
  CalculateLayoutSource.SET_SIZE_SPEC_ASYNC,
  CalculateLayoutSource.UPDATE_STATE_SYNC,
  CalculateLayoutSource.UPDATE_STATE_ASYNC,
  CalculateLayoutSource.MEASURE_SET_SIZE_SPEC,
  CalculateLayoutSource.MEASURE_SET_SIZE_SPEC_ASYNC,
  CalculateLayoutSource.RELOAD_PREVIOUS_STATE,
})
public @interface CalculateLayoutSource {
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
