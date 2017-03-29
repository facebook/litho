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
import java.util.Arrays;
import java.util.List;

import com.facebook.litho.javapoet.JPUtil;
import com.facebook.litho.specmodels.model.ClassNames;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

class PropsBuilderMethodsSpecBuilder {
  private int mIndex;
  private PropParameter mPropParameter;
  private String mImplName;
  private String mRequiredSetName;
  private TypeName mBuilderClass;
  private ClassName mComponentClass;

  PropsBuilderMethodsSpecBuilder index(int index) {
    this.mIndex = index;
    return this;
  }

  PropsBuilderMethodsSpecBuilder propParameter(PropParameter propParameter) {
    this.mPropParameter = propParameter;
    return this;
  }

  PropsBuilderMethodsSpecBuilder componentClassName(ClassName componentClass) {
    this.mComponentClass = componentClass;
    return this;
  }

  PropsBuilderMethodsSpecBuilder implName(String implName) {
    this.mImplName = implName;
    return this;
  }

  PropsBuilderMethodsSpecBuilder requiredSetName(String requiredSetName) {
    this.mRequiredSetName = requiredSetName;
    return this;
  }

  PropsBuilderMethodsSpecBuilder builderClass(TypeName builderClass) {
    this.mBuilderClass = builderClass;
    return this;
  }

  List<MethodSpec> build() {
    final List<MethodSpec> builders = new ArrayList<>();

    switch (mPropParameter.resType) {
      case STRING:
