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
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.Modifier;

class DelegateMethodExtractorTestHelper {

  static void assertDelegateMethodExtraction(
      ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods) {
    assertThat(delegateMethods.size()).isEqualTo(1);

    SpecMethodModel<DelegateMethod, Void> delegateMethod = delegateMethods.iterator().next();
    assertThat(delegateMethod.annotations.size()).isEqualTo(1);

    assertThat(delegateMethod.modifiers.size()).isEqualTo(1);

    assertThat(delegateMethod.modifiers.contains(Modifier.PUBLIC)).isTrue();
    assertThat(delegateMethod.name.toString()).isEqualTo("testMethod");

    assertThat(delegateMethod.returnType).isEqualTo(TypeName.VOID);

    assertThat(delegateMethod.methodParams.size()).isEqualTo(3);

    assertThat(delegateMethod.methodParams.get(0).getName()).isEqualTo("testProp");
    assertThat(delegateMethod.methodParams.get(0).getTypeName()).isEqualTo(TypeName.BOOLEAN);
    assertThat(delegateMethod.methodParams.get(0).getAnnotations().size()).isEqualTo(1);

    assertThat(delegateMethod.methodParams.get(1).getName()).isEqualTo("testState");
    assertThat(delegateMethod.methodParams.get(1).getTypeName()).isEqualTo(TypeName.INT);
    assertThat(delegateMethod.methodParams.get(1).getAnnotations().size()).isEqualTo(1);

    assertThat(delegateMethod.methodParams.get(2).getName()).isEqualTo("testPermittedAnnotation");
    assertThat(delegateMethod.methodParams.get(2).getTypeName())
        .isEqualTo(ClassName.bestGuess("java.lang.Object"));
    assertThat(delegateMethod.methodParams.get(2).getAnnotations().size()).isEqualTo(1);
  }
}
