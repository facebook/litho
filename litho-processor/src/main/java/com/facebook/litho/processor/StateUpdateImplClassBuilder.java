/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.List;

import com.facebook.litho.annotations.Param;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.SPEC_INSTANCE_NAME;

/**
 * Provides a Builder for generating an implementation of
 * {@link com.facebook.litho.ComponentLifecycle.StateUpdate}
 */
public class StateUpdateImplClassBuilder {

  private static final String STATE_CONTAINER_PARAM_NAME = "stateContainer";
  private static final String STATE_CONTAINER_IMPL_NAME = "stateContainerImpl";
  private static final String STATE_UPDATE_OLD_COMPONENT_NAME = "oldComponent";
  private static final String STATE_UPDATE_NEW_COMPONENT_NAME = "newComponent";
  private static final String STATE_UPDATE_IMPL_NAME_SUFFIX = "StateUpdate";
  private static final String STATE_UPDATE_METHOD_NAME = "updateState";
  private static final String STATE_UPDATE_IS_LAZY_METHOD_NAME = "isLazyStateUpdate";

