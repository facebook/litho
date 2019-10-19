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

import org.junit.Assert;
import org.junit.Test;

public class PsiTypeUtilsTest {

  @Test
  public void guessClassName() {
    Assert.assertEquals("T", PsiTypeUtils.guessClassName("T").reflectionName());
    Assert.assertEquals("com.Hello", PsiTypeUtils.guessClassName("com.Hello").reflectionName());
  }

  @Test
  public void getWildcardTypeName() {
    Assert.assertEquals("?", PsiTypeUtils.getWildcardTypeName("?").toString());
    Assert.assertEquals("? super T", PsiTypeUtils.getWildcardTypeName("? super T").toString());
    Assert.assertEquals(
        "? extends com.some.Node",
        PsiTypeUtils.getWildcardTypeName("? extends com.some.Node").toString());
    Assert.assertEquals(
        "? super com.A", PsiTypeUtils.getWildcardTypeName("? super com.A & com.B").toString());
  }
}
