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
package com.facebook.litho.intellij.completion;

import com.facebook.litho.intellij.LithoPluginTestHelper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OnEventGenerateUtilsTest {

  private final LithoPluginTestHelper testHelper = new LithoPluginTestHelper("testdata/completion");

  @Before
  public void setUp() throws Exception {
    testHelper.setUp();
  }

  @After
  public void tearDown() throws Exception {
    testHelper.tearDown();
  }

  @Test
  public void createOnEventMethod() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass psiEvent = psiClasses.get(0);

          PsiMethod onEventMethod =
              OnEventGenerateUtils.createOnEventMethod(psiEvent, psiEvent, Collections.emptyList());

          Assert.assertEquals(2, onEventMethod.getParameters().length);
          Assert.assertEquals("boolean", onEventMethod.getReturnType().getCanonicalText());

          return true;
        },
        "ReturnEvent.java");
  }
}
