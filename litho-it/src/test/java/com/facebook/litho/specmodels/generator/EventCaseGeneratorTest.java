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

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.TypeSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link EventCaseGenerator} */
@RunWith(JUnit4.class)
public class EventCaseGeneratorTest {
  @Test
  public void testBasicGeneratorCase() {
    final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("method");
    final EventDeclarationModel model =
        new EventDeclarationModel(ClassName.OBJECT, TypeName.VOID, ImmutableList.of(), null);

    EventCaseGenerator.builder()
        .contextClass(ClassNames.COMPONENT_CONTEXT)
        .eventMethodModels(
            ImmutableList.of(
                SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
                    .name("event")
                    .returnTypeSpec(new TypeSpec(TypeName.VOID))
                    .typeModel(model)
                    .build()))
        .writeTo(methodBuilder);

    assertThat(methodBuilder.build().toString())
        .isEqualTo(
            "void method() {\n"
                + "  case 96891546: {\n"
                + "    java.lang.Object _event = (java.lang.Object) eventState;\n"
                + "    event(\n"
                + "          eventHandler.mHasEventDispatcher);\n"
                + "    return null;\n"
                + "  }\n"
                + "}\n");
  }
}
