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

import java.util.List;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

/**
 * Builder for the shallowCopy method of a Component
 */
class ShallowCopyMethodSpecBuilder {
  private boolean mHasDeepCopy;
  private String mImplClassName;
  private List<String> mComponentsToCopy;
  private List<String> mInterStageVariables;
  private String mStateContainerImplClassName;

  ShallowCopyMethodSpecBuilder implClassName(String implClassName) {
    mImplClassName = implClassName;
    return this;
  }

  ShallowCopyMethodSpecBuilder hasDeepCopy(boolean hasDeepCopy) {
    mHasDeepCopy = hasDeepCopy;
    return this;
  }

  ShallowCopyMethodSpecBuilder componentsInImpl(List<String> componentsInImpl) {
    mComponentsToCopy = componentsInImpl;
    return this;
  }

  ShallowCopyMethodSpecBuilder interStageVariables(List<String> interStageVariables) {
    mInterStageVariables = interStageVariables;
    return this;
  }

  ShallowCopyMethodSpecBuilder stateContainerImplClassName(String stateContainerImplClassName) {
    mStateContainerImplClassName = stateContainerImplClassName;
    return this;
  }

  MethodSpec build() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("makeShallowCopy")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ClassName.bestGuess(mImplClassName));

    String deepCopy = mHasDeepCopy ? "deepCopy" : "";

    if (mHasDeepCopy) {
      builder.addParameter(ParameterSpec.builder(TypeName.BOOLEAN, "deepCopy").build());
    }

    builder.addStatement(
        "$L $L = ($L) super.makeShallowCopy($L)",
        mImplClassName,
        "component",
        mImplClassName,
        deepCopy);

    for (String element : mComponentsToCopy) {
      builder.addStatement(
          "component.$L = component.$L != null ? component.$L.makeShallowCopy($L) : null",
          element,
          element,
          element,
          deepCopy);
    }

    if (mHasDeepCopy) {
      builder.beginControlFlow("if (!deepCopy)");
    }

    for (String s : mInterStageVariables) {
      builder.addStatement("component.$L = null", s);
    }

    if (mStateContainerImplClassName != null) {
      builder.addStatement(
          "component." + Stages.STATE_CONTAINER_IMPL_MEMBER + " = new $T()",
          ClassName.bestGuess(mStateContainerImplClassName));
