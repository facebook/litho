/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

/**
 * Constants to be used when generating a Component.
 */
public interface GeneratorConstants {
  String DELEGATE_FIELD_NAME = "mSpec";
  String ABSTRACT_PARAM_NAME = "_abstract";
  String REF_VARIABLE_NAME = "_ref";
  String STATE_CONTAINER_FIELD_NAME = "mStateContainer";
  String PREVIOUS_RENDER_DATA_FIELD_NAME = "mPreviousRenderData";
  String STATE_CONTAINER_NAME_SUFFIX = "StateContainer";
  String STATE_UPDATE_NAME_SUFFIX = "StateUpdate";
}
