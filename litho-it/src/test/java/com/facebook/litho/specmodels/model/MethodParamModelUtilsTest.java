/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.lang.annotation.Annotation;
import org.junit.Test;

/**
 * Tests {@link MethodParamModelUtils}
 */
public class MethodParamModelUtilsTest {

  @Test
  public void testIsAnnotatedWith() {
    MethodParamModel methodParam =
        MockMethodParamModel.newBuilder().annotations(Override.class).build();
    assertThat(MethodParamModelUtils.isAnnotatedWith(methodParam, Override.class)).isTrue();
    assertThat(
        MethodParamModelUtils.isAnnotatedWith(methodParam, SuppressWarnings.class)).isFalse();
  }

  @Test
  public void testGetAnnotation() {
    MethodParamModel methodParam =
        MockMethodParamModel.newBuilder().annotations(Override.class).build();
    Annotation overrideAnnotation = methodParam.getAnnotations().get(0);
    assertThat(MethodParamModelUtils.getAnnotation(methodParam, Override.class))
        .isEqualTo(overrideAnnotation);
  }

  @Test
  public void testGetTypeVariablesOnClassName() {
    TypeName objectClass = ClassName.bestGuess("java.lang.Object");
    assertThat(MethodParamModelUtils.getTypeVariables(objectClass)).isEmpty();
  }

  @Test
  public void testGetTypeVariablesOnTypeVariableName() {
    TypeVariableName type = TypeVariableName.get("T");
    assertThat(MethodParamModelUtils.getTypeVariables(type)).hasSize(1);
    assertThat(MethodParamModelUtils.getTypeVariables(type)).contains(type);
  }

  @Test
  public void testGetTypeVariablesOnParameterizedTypeRecursive() {
    TypeVariableName type1 = TypeVariableName.get("R");
    TypeVariableName type2 = TypeVariableName.get("S");
    TypeVariableName type3 = TypeVariableName.get("T");

    TypeName type =
        ParameterizedTypeName.get(
            ClassName.bestGuess("java.lang.Object"),
            type1,
            type2,
            ParameterizedTypeName.get(ClassName.bestGuess("java.lang.Object"), type3));
    assertThat(MethodParamModelUtils.getTypeVariables(type)).hasSize(3);
    assertThat(MethodParamModelUtils.getTypeVariables(type)).contains(type1);
    assertThat(MethodParamModelUtils.getTypeVariables(type)).contains(type2);
    assertThat(MethodParamModelUtils.getTypeVariables(type)).contains(type3);
  }

}
