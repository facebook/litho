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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

class OnCreateMountContentMethodBuilder {

  private String mTarget;
  private String mDelegateMethodName;

  OnCreateMountContentMethodBuilder target(String target) {
    mTarget = target;
    return this;
  }

  OnCreateMountContentMethodBuilder delegateName(String delegateMethodName) {
    mDelegateMethodName = delegateMethodName;
    return this;
  }

  MethodSpec build() {
    return  MethodSpec.methodBuilder("onCreateMountContent")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(
            ParameterSpec.builder(ClassNames.COMPONENT_CONTEXT, "context").build())
        .returns(ClassName.OBJECT)
