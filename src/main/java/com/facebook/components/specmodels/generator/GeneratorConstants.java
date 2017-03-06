// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.generator;

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
