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

import com.facebook.litho.specmodels.model.ClassNames;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Builder for the lazy static state update methods of a Component.
 */
class OnLazyStateUpdateMethodSpecBuilder {

  private static final String STATE_UPDATE_METHOD_NAME = "updateState";
  private static final String STATE_UPDATE_NEW_COMPONENT_NAME = "newComponent";
  private static final String STATE_CONTAINER_PARAM_NAME = "stateContainer";
  private static final String STATE_UPDATE_IMPL_NAME_SUFFIX = "StateUpdate";
  private static final String STATE_UPDATE_VALUE_PARAM = "lazyUpdateValue";
