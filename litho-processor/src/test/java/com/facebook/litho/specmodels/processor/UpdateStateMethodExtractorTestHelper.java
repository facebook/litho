/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.UpdateStateMethod;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

public class UpdateStateMethodExtractorTestHelper {
  static void assertMethodExtraction(
      ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> methods) {
    assertThat(methods.size()).isEqualTo(1);

    SpecMethodModel<UpdateStateMethod, Void> updateStateMethod = methods.iterator().next();
    List<Annotation> onUpdateStateAnnotations =
        updateStateMethod.annotations.stream()
            .filter(x -> x instanceof OnUpdateState)
            .collect(Collectors.toList());
    assertThat(onUpdateStateAnnotations.isEmpty()).isFalse();

    assertThat(updateStateMethod.modifiers.size()).isEqualTo(1);
    assertThat(updateStateMethod.modifiers.contains(Modifier.PUBLIC)).isTrue();

    assertThat(updateStateMethod.name.toString()).isEqualTo("testMethod");

    assertThat(updateStateMethod.returnType).isEqualTo(TypeName.VOID);

    assertThat(updateStateMethod.methodParams.size()).isEqualTo(3);

    assertThat(updateStateMethod.methodParams.get(0).getName()).isEqualTo("testProp");
    assertThat(updateStateMethod.methodParams.get(0).getTypeName()).isEqualTo(TypeName.BOOLEAN);
    assertThat(updateStateMethod.methodParams.get(0).getAnnotations().size()).isEqualTo(1);

    assertThat(updateStateMethod.methodParams.get(1).getName()).isEqualTo("testState");
    assertThat(updateStateMethod.methodParams.get(1).getTypeName()).isEqualTo(TypeName.INT);
    assertThat(updateStateMethod.methodParams.get(1).getAnnotations().size()).isEqualTo(1);

    assertThat(updateStateMethod.methodParams.get(2).getName())
        .isEqualTo("testPermittedAnnotation");
    assertThat(updateStateMethod.methodParams.get(2).getTypeName())
        .isEqualTo(ClassName.bestGuess("java.lang.Object"));
    assertThat(updateStateMethod.methodParams.get(2).getAnnotations().size()).isEqualTo(1);
  }
}
