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
  private static final String STATE_UPDATE_IS_LAZY_METHOD_NAME = "isLazyStateUpdate";

  private TypeName mContextClass;
  private ClassName mComponentClass;
  private String mImplClass;

  private String mStateName;
  private TypeName mStateUpdateType;
  private TypeName mStateContainerClassName;
  private TypeName mStateType;
  private String mLifecycleImplClassName;

  OnLazyStateUpdateMethodSpecBuilder contextClass(TypeName contextClass) {
    this.mContextClass = contextClass;
    return this;
  }

  OnLazyStateUpdateMethodSpecBuilder componentClass(ClassName componentClass) {
    mComponentClass = componentClass;
    return this;
  }

  OnLazyStateUpdateMethodSpecBuilder implClass(String implClass) {
    mImplClass = implClass;
    return this;
  }

  OnLazyStateUpdateMethodSpecBuilder stateName(String stateName) {
    mStateName = stateName;
    return this;
  }

  OnLazyStateUpdateMethodSpecBuilder stateType(TypeName stateType) {
    mStateType = stateType;
    return this;
  }

  OnLazyStateUpdateMethodSpecBuilder stateUpdateType(TypeName stateUpdateType) {
    mStateUpdateType = stateUpdateType;
    return this;
  }

  public OnLazyStateUpdateMethodSpecBuilder withStateContainerClassName(TypeName className) {
    mStateContainerClassName = className;
    return this;
  }

  public OnLazyStateUpdateMethodSpecBuilder lifecycleImplClass(String lifecycleImplClassName) {
    mLifecycleImplClassName = lifecycleImplClassName;
    return this;
  }

  MethodSpec build() {

    final String newComponentImplName =
        STATE_UPDATE_NEW_COMPONENT_NAME + STATE_UPDATE_IMPL_NAME_SUFFIX;

    final MethodSpec.Builder builder = MethodSpec.methodBuilder(
        "lazyUpdate" + mStateName.substring(0, 1).toUpperCase() + mStateName.substring(1))
        .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
        .addParameter(mContextClass, "c")
        .addParameter(mStateType, STATE_UPDATE_VALUE_PARAM, Modifier.FINAL);

    builder.addStatement(
        "$T _component = c.get$LScope()",
        mComponentClass,
        mComponentClass.simpleName())
        .addCode(
            CodeBlock.builder()
                .beginControlFlow("if (_component == null)")
                .addStatement("return")
                .endControlFlow()
                .build());

    final TypeName implClass = ClassName.bestGuess(mLifecycleImplClassName + "." + mImplClass);
    final TypeName boxedStateType = mStateType.isPrimitive() ? mStateType.box() : mStateType;

    final MethodSpec.Builder stateUpdate = MethodSpec.methodBuilder(STATE_UPDATE_METHOD_NAME)
        .addParameter(mStateContainerClassName, STATE_CONTAINER_PARAM_NAME)
        .addParameter(mComponentClass, STATE_UPDATE_NEW_COMPONENT_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addStatement(
            "$T " + newComponentImplName + " = ($T) " + STATE_UPDATE_NEW_COMPONENT_NAME,
            implClass,
            implClass)
        .addStatement(
            "$T " + mStateName + " = new $T()",
            ParameterizedTypeName.get(
                ClassNames.STATE_VALUE,
                boxedStateType),
            ParameterizedTypeName.get(
                ClassNames.STATE_VALUE,
                boxedStateType))
        .addStatement(
            mStateName + ".set(" + STATE_UPDATE_VALUE_PARAM + ")")
        .addStatement(newComponentImplName +
            "." + Stages.STATE_CONTAINER_IMPL_MEMBER +
            "." + mStateName +
            " = " + mStateName + ".get()");

    final MethodSpec.Builder isLazyStateUpdate =
        MethodSpec.methodBuilder(STATE_UPDATE_IS_LAZY_METHOD_NAME)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.BOOLEAN)
            .addStatement("return true");

    final TypeSpec.Builder stateBuilderImpl = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(mStateUpdateType)
        .addMethod(stateUpdate.build())
        .addMethod(isLazyStateUpdate.build());
