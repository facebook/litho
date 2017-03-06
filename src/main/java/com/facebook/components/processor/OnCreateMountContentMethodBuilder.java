// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.lang.model.element.Modifier;

import com.facebook.components.specmodels.model.ClassNames;

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
        .addStatement("return this.$L.$L(context)", mTarget, mDelegateMethodName)
        .build();
  }
}
