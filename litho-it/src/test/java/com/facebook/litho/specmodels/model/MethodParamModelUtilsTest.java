/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link MethodParamModelUtils} */
@RunWith(JUnit4.class)
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
