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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.Modifier;

class TriggerMethodExtractorTestHelper {

  static void assertMethodExtraction(
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> methods,
      ClassName className) {

    assertThat(methods).hasSize(1);

    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod = methods.get(0);
    assertThat(eventMethod.typeModel).isNotNull();
    assertThat(eventMethod.typeModel.name).isEqualTo(className);

    assertThat(eventMethod.modifiers).hasSize(1);
    assertThat(eventMethod.modifiers).contains(Modifier.PUBLIC);
    assertThat(eventMethod.name.toString()).isEqualTo("testMethod");
    assertThat(eventMethod.returnType).isEqualTo(TypeName.VOID);
    assertThat(eventMethod.methodParams).hasSize(4);

    MethodParamModel firstParam = eventMethod.methodParams.get(0),
        secondParam = eventMethod.methodParams.get(1),
        thirdParam = eventMethod.methodParams.get(2),
        fourthParam = eventMethod.methodParams.get(3);

    assertThat(firstParam.getName()).isEqualTo("testProp");
    assertThat(firstParam.getTypeName()).isEqualTo(TypeName.BOOLEAN);
    assertThat(firstParam.getAnnotations()).hasSize(1);

    assertThat(secondParam.getName()).isEqualTo("testState");
    assertThat(secondParam.getTypeName()).isEqualTo(TypeName.INT);
    assertThat(secondParam.getAnnotations()).hasSize(1);

    assertThat(thirdParam.getName()).isEqualTo("testPermittedAnnotation");
    assertThat(thirdParam.getTypeName()).isEqualTo(ClassName.bestGuess("java.lang.Object"));
    assertThat(thirdParam.getAnnotations()).hasSize(1);

    assertThat(fourthParam.getName()).isEqualTo("arg4");
    assertThat(fourthParam.getTypeName()).isEqualTo(TypeName.LONG);
    assertThat(fourthParam.getAnnotations()).hasSize(1);
  }
}
