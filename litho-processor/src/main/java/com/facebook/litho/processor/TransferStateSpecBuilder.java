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

import java.util.LinkedHashSet;
import java.util.Set;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

class TransferStateSpecBuilder {

  private Set<String> mStateParameters = new LinkedHashSet<>();
  private ClassName mContextClassName;
  private ClassName mStateContainerClassName;
  private ClassName mComponentClassName;
  private String mComponentImplClassName;
  private String mStateContainerImplClassName;

  TransferStateSpecBuilder contextClassName(ClassName contextClassName) {
    mContextClassName = contextClassName;
    return this;
  }

  TransferStateSpecBuilder stateParameters(Set<String> stateParameters) {
    if (stateParameters != null) {
      mStateParameters = stateParameters;
    }
    return this;
  }

  TransferStateSpecBuilder componentClassName(ClassName componentClassName) {
    mComponentClassName = componentClassName;
    return this;
  }

  TransferStateSpecBuilder stateContainerClassName(ClassName stateContainerClassName) {
    mStateContainerClassName = stateContainerClassName;
    return this;
  }

  TransferStateSpecBuilder componentImplClassName(String componentImplClassName) {
    mComponentImplClassName = componentImplClassName;
    return this;
  }

  TransferStateSpecBuilder stateContainerImplClassName(String stateContainerImplClassName) {
    mStateContainerImplClassName = stateContainerImplClassName;
    return this;
  }

  MethodSpec build() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("transferState")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(ParameterSpec.builder(mContextClassName, "context").build())
        .addParameter(
            ParameterSpec.builder(mStateContainerClassName, "prevStateContainer").build())
        .addParameter(
            ParameterSpec.builder(mComponentClassName, "component").build())
