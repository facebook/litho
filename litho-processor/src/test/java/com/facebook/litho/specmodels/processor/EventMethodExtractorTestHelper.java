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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.Modifier;

public class EventMethodExtractorTestHelper {
  static void assertMethodExtraction(
      List<SpecMethodModel<EventMethod, EventDeclarationModel>> methods) {
    assertThat(methods.size()).isEqualTo(1);

    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod = methods.iterator().next();

    ImmutableList<TypeVariableName> typeVariables = eventMethod.typeVariables;
    assertThat(typeVariables.size()).isEqualTo(1);
    TypeVariableName typeVariable = typeVariables.get(0);
    assertThat(typeVariable.name).isEqualTo("T");
    assertThat(typeVariable.bounds.size()).isEqualTo(1);
    assertThat(typeVariable.bounds.get(0).toString()).isEqualTo("java.lang.Integer");

    assertThat(eventMethod.typeModel).isNotNull();
    assertThat(eventMethod.typeModel.name).isEqualTo(ClassName.bestGuess("java.lang.Object"));

    assertThat(eventMethod.modifiers.size()).isEqualTo(1);
    assertThat(eventMethod.modifiers.get(0)).isEqualTo(Modifier.PUBLIC);

    assertThat(eventMethod.name.toString()).isEqualTo("testMethod");

    assertThat(eventMethod.returnType).isEqualTo(TypeName.VOID);

    assertThat(eventMethod.methodParams.size()).isEqualTo(5);

    MethodParamModel testProp = eventMethod.methodParams.get(0);
    assertThat(testProp.getName()).isEqualTo("testProp");
    assertThat(testProp.getTypeName()).isEqualTo(TypeName.BOOLEAN);
    assertThat(testProp.getAnnotations().size()).isEqualTo(1);

    MethodParamModel testState = eventMethod.methodParams.get(1);
    assertThat(testState.getName()).isEqualTo("testState");
    assertThat(testState.getTypeName()).isEqualTo(TypeName.INT);
    assertThat(testState.getAnnotations().size()).isEqualTo(1);

    MethodParamModel testPermittedAnnotation = eventMethod.methodParams.get(2);
    assertThat(testPermittedAnnotation.getName()).isEqualTo("testPermittedAnnotation");
    assertThat(testPermittedAnnotation.getTypeName())
        .isEqualTo(ClassName.bestGuess("java.lang.Object"));
    assertThat(testPermittedAnnotation.getAnnotations().size()).isEqualTo(1);

    MethodParamModel testNotPermittedAnnotation = eventMethod.methodParams.get(3);
    assertThat(testNotPermittedAnnotation.getName()).isEqualTo("testNotPermittedAnnotation");
    assertThat(testNotPermittedAnnotation.getAnnotations().size()).isEqualTo(0);
  }
}
