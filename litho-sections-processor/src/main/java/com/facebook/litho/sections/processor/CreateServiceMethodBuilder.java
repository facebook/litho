/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import javax.lang.model.element.Modifier;

class CreateServiceMethodBuilder {

  private static final String ABSTRACT_IMPL_INSTANCE_NAME = "_abstractImpl";
  private static final String IMPL_INSTANCE_NAME = "_impl";

  private String mImplClassName;
  private String mServiceInstanceName;
  private String mOnCreateServiceMethodName;

  CreateServiceMethodBuilder implClassName(String implClassName) {
    mImplClassName = implClassName;
    return this;
  }

  CreateServiceMethodBuilder serviceInstanceName(String serviceInstanceName) {
    mServiceInstanceName = serviceInstanceName;
    return this;
  }

  CreateServiceMethodBuilder delegateMethodName(String onCreateServiceMethodName) {
    mOnCreateServiceMethodName = onCreateServiceMethodName;
    return this;
  }

  MethodSpec build() {
    return MethodSpec.methodBuilder("createService")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(
            ParameterSpec.builder(SectionClassNames.SECTION_CONTEXT, "context").build())
        .addParameter(
            ParameterSpec.builder(SectionClassNames.SECTION, ABSTRACT_IMPL_INSTANCE_NAME).build())
        .addStatement(mImplClassName +
            " " + IMPL_INSTANCE_NAME + " = (" +
            mImplClassName +
            ") " +
            ABSTRACT_IMPL_INSTANCE_NAME)
        .addStatement(IMPL_INSTANCE_NAME +
            "." +
            mServiceInstanceName +
            " = " +
            mOnCreateServiceMethodName +
            "(context, " +
            ABSTRACT_IMPL_INSTANCE_NAME +
            ")")
        .build();
  }
}
