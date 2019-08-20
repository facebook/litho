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

import org.junit.Assert;
import org.junit.Test;

public class PsiTypeUtilsTest {

  @Test
  public void guessClassName() {
    Assert.assertEquals("Object", PsiTypeUtils.guessClassName("?").simpleName());
    Assert.assertEquals("Object", PsiTypeUtils.guessClassName("T").simpleName());
    Assert.assertEquals("Hello", PsiTypeUtils.guessClassName("Hello").simpleName());
    Assert.assertEquals(
        "com.some.Node", PsiTypeUtils.guessClassName("? extends com.some.Node").reflectionName());
    Assert.assertEquals(
        "Collection", PsiTypeUtils.guessClassName("Collection<? extends T>").simpleName());
    Assert.assertEquals(
        "ClassA", PsiTypeUtils.guessClassName("T extends ClassA & InterfaceB").simpleName());
    Assert.assertEquals(
        "ClassA", PsiTypeUtils.guessClassName("? extends ClassA<InterfaceB>").simpleName());
  }
}
