/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.litho.specmodels.processor;

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
import org.junit.Assert;

public class UpdateStateMethodExtractorTestHelper {
  static void assertMethodExtraction(
      ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> methods) {
    Assert.assertEquals(1, methods.size());

    SpecMethodModel<UpdateStateMethod, Void> updateStateMethod = methods.iterator().next();
    List<Annotation> onUpdateStateAnnotations =
        updateStateMethod.annotations.stream()
            .filter(x -> x instanceof OnUpdateState)
            .collect(Collectors.toList());
    Assert.assertFalse(onUpdateStateAnnotations.isEmpty());

    Assert.assertEquals(1, updateStateMethod.modifiers.size());
    Assert.assertTrue(updateStateMethod.modifiers.contains(Modifier.PUBLIC));

    Assert.assertEquals("testMethod", updateStateMethod.name.toString());

    Assert.assertEquals(TypeName.VOID, updateStateMethod.returnType);

    Assert.assertEquals(3, updateStateMethod.methodParams.size());

    Assert.assertEquals("testProp", updateStateMethod.methodParams.get(0).getName());
    Assert.assertEquals(TypeName.BOOLEAN, updateStateMethod.methodParams.get(0).getTypeName());
    Assert.assertEquals(1, updateStateMethod.methodParams.get(0).getAnnotations().size());

    Assert.assertEquals("testState", updateStateMethod.methodParams.get(1).getName());
    Assert.assertEquals(TypeName.INT, updateStateMethod.methodParams.get(1).getTypeName());
    Assert.assertEquals(1, updateStateMethod.methodParams.get(1).getAnnotations().size());

    Assert.assertEquals("testPermittedAnnotation", updateStateMethod.methodParams.get(2).getName());
    Assert.assertEquals(
        ClassName.bestGuess("java.lang.Object"),
        updateStateMethod.methodParams.get(2).getTypeName());
    Assert.assertEquals(1, updateStateMethod.methodParams.get(2).getAnnotations().size());
  }
}
