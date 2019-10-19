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

import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Assert;

public class EventMethodExtractorTestHelper {
  static void assertMethodExtraction(
      List<SpecMethodModel<EventMethod, EventDeclarationModel>> methods,
      int expectedNumAnnotations) {
    Assert.assertEquals(methods.size(), 1);

    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod = methods.iterator().next();
    Assert.assertNotNull(eventMethod.typeModel);
    Assert.assertEquals(eventMethod.typeModel.name, ClassName.bestGuess("java.lang.Object"));

    Assert.assertEquals(eventMethod.modifiers.size(), 1);
    Assert.assertEquals(eventMethod.modifiers.get(0), Modifier.PUBLIC);

    Assert.assertEquals(eventMethod.name.toString(), "testMethod");

    Assert.assertEquals(eventMethod.returnType, TypeName.VOID);

    Assert.assertEquals(eventMethod.methodParams.size(), 3);

    Assert.assertEquals(eventMethod.methodParams.get(0).getName(), "testProp");
    Assert.assertEquals(eventMethod.methodParams.get(0).getTypeName(), TypeName.BOOLEAN);
    Assert.assertEquals(eventMethod.methodParams.get(0).getAnnotations().size(), 1);

    Assert.assertEquals(eventMethod.methodParams.get(1).getName(), "testState");
    Assert.assertEquals(eventMethod.methodParams.get(1).getTypeName(), TypeName.INT);
    Assert.assertEquals(eventMethod.methodParams.get(1).getAnnotations().size(), 1);

    Assert.assertEquals(eventMethod.methodParams.get(2).getName(), "testPermittedAnnotation");
    Assert.assertEquals(
        eventMethod.methodParams.get(2).getTypeName(), ClassName.bestGuess("java.lang.Object"));
    Assert.assertEquals(
        eventMethod.methodParams.get(2).getAnnotations().size(), expectedNumAnnotations);
  }
}
