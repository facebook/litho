/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.model.MethodParamModel;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;

public final class MethodExtractorUtilsTestHelper {
  private MethodExtractorUtilsTestHelper() {};

  public static void assertOnDetachedHasNoParams(List<MethodParamModel> onDetachedMethodParams) {
    assertThat(onDetachedMethodParams).isEmpty();
  }

  public static void assertOnAttachedHasInfoForAllParams(
      List<MethodParamModel> onAttachedMethodParams) {
    assertThat(onAttachedMethodParams).hasSize(3);

    assertThat(onAttachedMethodParams.get(0).getName()).isEqualTo("c");
    assertThat(onAttachedMethodParams.get(0).getTypeName().toString())
        .isEqualTo("com.facebook.litho.ComponentContext");
    assertThat(onAttachedMethodParams.get(0).getAnnotations()).isEmpty();

    assertThat(onAttachedMethodParams.get(1).getName()).isEqualTo("num");
    assertThat(onAttachedMethodParams.get(1).getTypeName().toString()).isEqualTo("int");
    assertThat(onAttachedMethodParams.get(1).getAnnotations()).isEmpty();

    assertThat(onAttachedMethodParams.get(2).getName()).isEqualTo("prop");
    assertThat(onAttachedMethodParams.get(2).getTypeName().toString())
        .isEqualTo("java.lang.Object");
    assertThat(onAttachedMethodParams.get(2).getAnnotations()).hasSize(1);
    assertThat(onAttachedMethodParams.get(2).getAnnotations().get(0).annotationType().getTypeName())
        .isEqualTo("com.facebook.litho.annotations.Prop");
  }

  public static void assertOnAttachedHasNoTypeVars(List<TypeVariableName> onAttachedTypeVariables) {
    assertThat(onAttachedTypeVariables).isEmpty();
  }

  public static void assertOnDetachedHasInfoForAllTypeVars(
      List<TypeVariableName> onDetachedTypeVars) {
    assertThat(onDetachedTypeVars).hasSize(2);

    assertThat(onDetachedTypeVars.get(0).name).isEqualTo("T");
    assertThat(onDetachedTypeVars.get(0).bounds).isEmpty();

    assertThat(onDetachedTypeVars.get(1).name).isEqualTo("U");
    assertThat(onDetachedTypeVars.get(1).bounds).hasSize(1);
    assertThat(onDetachedTypeVars.get(1).bounds.get(0).toString()).isEqualTo("java.util.List");
  }
}
