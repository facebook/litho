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

class EventHandlerFactoryMethodSpecBuilder {
  private String mEventName;
  private int mEventId;
  private TypeName mContextClass;
  private final List<Parameter> mEventParams = new ArrayList<>();
  private TypeName mEventHandlerClassName;
  private final ArrayList<String> mTypeParameters = new ArrayList<>();

  EventHandlerFactoryMethodSpecBuilder eventName(String eventName) {
    this.mEventName = eventName;
    return this;
  }

  EventHandlerFactoryMethodSpecBuilder eventId(int eventId) {
    this.mEventId = eventId;
    return this;
  }

  EventHandlerFactoryMethodSpecBuilder contextClass(TypeName contextClass) {
    this.mContextClass = contextClass;
    return this;
  }

  EventHandlerFactoryMethodSpecBuilder eventParams(List<Parameter> eventParams) {
    this.mEventParams.addAll(eventParams);
    return this;
  }

  EventHandlerFactoryMethodSpecBuilder eventParam(Parameter eventParam) {
    this.mEventParams.add(eventParam);
    return this;
  }

  EventHandlerFactoryMethodSpecBuilder eventHandlerClassName(TypeName eventHandlerClassName) {
    mEventHandlerClassName = eventHandlerClassName;
    return this;
  }

  EventHandlerFactoryMethodSpecBuilder typeParameter(String typeParam) {
    mTypeParameters.add(typeParam);
    return this;
  }

  EventHandlerFactoryMethodSpecBuilder typeParameters(List<String> typeParams) {
    mTypeParameters.addAll(typeParams);
    return this;
  }

  MethodSpec build() {
    final MethodSpec.Builder builder = MethodSpec.methodBuilder(mEventName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(mContextClass, "c")
        .returns(mEventHandlerClassName);

    for (String typeParam : mTypeParameters) {
      builder.addTypeVariable(TypeVariableName.get(typeParam));
    }

