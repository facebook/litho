/**
 * Copyright (c) 2014-present, Facebook, Inc.
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
  String SPEC_INSTANCE_NAME = "sInstance";
  String ABSTRACT_IMPL_PARAM_NAME = "_abstractImpl";
  String IMPL_VARIABLE_NAME = "_impl";
  String IMPL_CLASS_NAME_SUFFIX = "Impl";
  String STATE_CONTAINER_FIELD_NAME = "mStateContainerImpl";
  String STATE_CONTAINER_IMPL_NAME_SUFFIX = "StateContainerImpl";
  String STATE_UPDATE_IMPL_NAME_SUFFIX = "StateUpdate";
}
