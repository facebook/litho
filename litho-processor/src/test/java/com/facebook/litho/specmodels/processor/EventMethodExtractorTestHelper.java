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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    assertEquals(methods.size(), 1);

    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod = methods.iterator().next();

    ImmutableList<TypeVariableName> typeVariables = eventMethod.typeVariables;
    assertEquals(1, typeVariables.size());
    TypeVariableName typeVariable = typeVariables.get(0);
    assertEquals("T", typeVariable.name);
    assertEquals(1, typeVariable.bounds.size());
    assertEquals("java.lang.Integer", typeVariable.bounds.get(0).toString());

    assertNotNull(eventMethod.typeModel);
    assertEquals(eventMethod.typeModel.name, ClassName.bestGuess("java.lang.Object"));

    assertEquals(eventMethod.modifiers.size(), 1);
    assertEquals(eventMethod.modifiers.get(0), Modifier.PUBLIC);

    assertEquals(eventMethod.name.toString(), "testMethod");

    assertEquals(eventMethod.returnType, TypeName.VOID);

    assertEquals(eventMethod.methodParams.size(), 5);

    MethodParamModel testProp = eventMethod.methodParams.get(0);
    assertEquals(testProp.getName(), "testProp");
    assertEquals(testProp.getTypeName(), TypeName.BOOLEAN);
    assertEquals(testProp.getAnnotations().size(), 1);

    MethodParamModel testState = eventMethod.methodParams.get(1);
    assertEquals(testState.getName(), "testState");
    assertEquals(testState.getTypeName(), TypeName.INT);
    assertEquals(testState.getAnnotations().size(), 1);

    MethodParamModel testPermittedAnnotation = eventMethod.methodParams.get(2);
    assertEquals(testPermittedAnnotation.getName(), "testPermittedAnnotation");
    assertEquals(testPermittedAnnotation.getTypeName(), ClassName.bestGuess("java.lang.Object"));
    assertEquals(1, testPermittedAnnotation.getAnnotations().size());

    MethodParamModel testNotPermittedAnnotation = eventMethod.methodParams.get(3);
    assertEquals(testNotPermittedAnnotation.getName(), "testNotPermittedAnnotation");
    assertEquals(0, testNotPermittedAnnotation.getAnnotations().size());
  }
}
