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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.Modifier;
import org.junit.Assert;

class PsiDelegateMethodExtractorTestHelper {

  static void assertDelegateMethodExtraction(
      ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods) {
    Assert.assertEquals(1, delegateMethods.size());

    SpecMethodModel<DelegateMethod, Void> delegateMethod = delegateMethods.iterator().next();
    Assert.assertEquals(1, delegateMethod.annotations.size());

    Assert.assertEquals(1, delegateMethod.modifiers.size());

    Assert.assertTrue(delegateMethod.modifiers.contains(Modifier.PUBLIC));
    Assert.assertEquals("testMethod", delegateMethod.name.toString());

    Assert.assertEquals(TypeName.VOID, delegateMethod.returnType);

    Assert.assertEquals(3, delegateMethod.methodParams.size());

    Assert.assertEquals("testProp", delegateMethod.methodParams.get(0).getName());
    Assert.assertEquals(TypeName.BOOLEAN, delegateMethod.methodParams.get(0).getTypeName());
    Assert.assertEquals(1, delegateMethod.methodParams.get(0).getAnnotations().size());

    Assert.assertEquals("testState", delegateMethod.methodParams.get(1).getName());
    Assert.assertEquals(TypeName.INT, delegateMethod.methodParams.get(1).getTypeName());
    Assert.assertEquals(1, delegateMethod.methodParams.get(1).getAnnotations().size());

    Assert.assertEquals("testPermittedAnnotation", delegateMethod.methodParams.get(2).getName());
    Assert.assertEquals(
        ClassName.bestGuess("java.lang.Object"), delegateMethod.methodParams.get(2).getTypeName());
    Assert.assertEquals(1, delegateMethod.methodParams.get(2).getAnnotations().size());
  }
}
