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

import static com.facebook.litho.specmodels.processor.MethodExtractorUtilsTestHelper.assertOnAttachedHasInfoForAllParams;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtilsTestHelper.assertOnAttachedHasNoTypeVars;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtilsTestHelper.assertOnDetachedHasInfoForAllTypeVars;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtilsTestHelper.assertOnDetachedHasNoParams;
import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getMethodParams;
import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getTypeVariables;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class PsiMethodExtractorUtilsTest extends LithoPluginIntellijTest {
  PsiMethod[] methods;

  public PsiMethodExtractorUtilsTest() {
    super("testdata/processor");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    final PsiFile psiFile = testHelper.configure("PsiMethodExtractorUtilsTest.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              methods =
                  LithoPluginUtils.getFirstClass(psiFile, cls -> "TestClass".equals(cls.getName()))
                      .get()
                      .getMethods();
            });
  }

  @Test
  public void getMethodParams_forMethodWithNoParams_returnsEmptyList() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertOnDetachedHasNoParams(
                  getMethodParams(
                      methods[1],
                      Collections.emptyList(),
                      Collections.emptyList(),
                      Collections.emptyList()));
            });
  }

  @Test
  public void getMethodParams_forMethodwithParams_returnsInfoForAllParams() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertOnAttachedHasInfoForAllParams(
                  getMethodParams(
                      methods[0],
                      Collections.singletonList(Prop.class),
                      Collections.emptyList(),
                      Collections.emptyList()));
            });
  }

  @Test
  public void getTypeVariables_forMethodWithNoTypeVars_returnsEmptyList() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertOnAttachedHasNoTypeVars(getTypeVariables(methods[0]));
            });
  }

  @Test
  public void getTypeVariables_forMethodWithTypeVars_returnsInfoForAllTypeVars() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertOnDetachedHasInfoForAllTypeVars(getTypeVariables(methods[1]));
            });
  }
}
