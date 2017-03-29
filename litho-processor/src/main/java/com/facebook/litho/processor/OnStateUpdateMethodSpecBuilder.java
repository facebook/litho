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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

/**
 * Builder for the static state update methods of a Component
 */
class OnStateUpdateMethodSpecBuilder {
  private String mUpdateMethodName;
  private TypeName mContextClass;
  private ClassName mComponentClass;
  private String mLifecycleImplClass;

  private final List<Parameter> mUpdateParams = new ArrayList<>();
  private final List<String> mTypeParameters = new ArrayList<>();
  private String mStateUpdateClassName;
  private boolean mIsAsync;

  OnStateUpdateMethodSpecBuilder updateMethodName(String updateMethodName) {
    this.mUpdateMethodName = updateMethodName;
    return this;
  }

  OnStateUpdateMethodSpecBuilder contextClass(TypeName contextClass) {
    this.mContextClass = contextClass;
    return this;
  }

  OnStateUpdateMethodSpecBuilder updateMethodParams(List<Parameter> eventParams) {
    this.mUpdateParams.addAll(eventParams);
    return this;
  }

  OnStateUpdateMethodSpecBuilder updateMethodParam(Parameter eventParam) {
    this.mUpdateParams.add(eventParam);
    return this;
  }

