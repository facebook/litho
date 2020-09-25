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

package com.facebook.litho.intellij.completion;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import java.util.Collections;
import org.junit.Test;

public class OnEventGenerateUtilsTest extends LithoPluginIntellijTest {
  private PsiFile psiFile;
  private PsiClass testEvent;

  public OnEventGenerateUtilsTest() {
    super("testdata/completion");
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    psiFile = testHelper.configure("OnEventGenerateUtilsTest.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              testEvent =
                  LithoPluginUtils.getFirstClass(psiFile, cls -> "TestEvent".equals(cls.getName()))
                      .get();
            });
  }

  @Test
  public void
      createOnEventMethod_whenNoMethodForGivenEventTypeExistsAndEventClassEmpty_createVoidMethodWithDefaultNameAndDefaultContextParam() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass testClassWithoutOnEventMethod =
                  LithoPluginUtils.getFirstClass(
                          psiFile, cls -> "TestClassWithoutOnEventMethod".equals(cls.getName()))
                      .get();
              final PsiMethod onEventMethod =
                  OnEventGenerateUtils.createOnEventMethod(
                      testClassWithoutOnEventMethod, testEvent, Collections.emptyList());
              assertThat(onEventMethod.getName()).isEqualTo("onTestEvent");

              final PsiParameter[] parameterList = onEventMethod.getParameterList().getParameters();
              assertThat(parameterList).hasSize(1);
              assertThat(parameterList[0].getType().getCanonicalText())
                  .isEqualTo(LithoClassNames.COMPONENT_CONTEXT_CLASS_NAME);
              assertThat(parameterList[0].getName()).isEqualTo("c");
            });
  }

  @Test
  public void
      createOnEventMethod_whenEventClassWithReturnTypeAndParameters_createMethodWithReturnTypeAndAdditionalParameters() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass testClassWithoutOnEventMethod =
                  LithoPluginUtils.getFirstClass(
                          psiFile, cls -> "TestClassWithoutOnEventMethod".equals(cls.getName()))
                      .get();
              final PsiClass testEventWithReturnTypeAndParam =
                  LithoPluginUtils.getFirstClass(
                          psiFile, cls -> "TestEventWithReturnTypeAndParam".equals(cls.getName()))
                      .get();
              final PsiMethod onEventMethod =
                  OnEventGenerateUtils.createOnEventMethod(
                      testClassWithoutOnEventMethod,
                      testEventWithReturnTypeAndParam,
                      Collections.emptyList());

              final PsiParameter[] parameterList = onEventMethod.getParameterList().getParameters();
              assertThat(parameterList).hasSize(2);
              assertThat(parameterList[0].getType().getCanonicalText())
                  .isEqualTo(LithoClassNames.COMPONENT_CONTEXT_CLASS_NAME);
              assertThat(parameterList[0].getName()).isEqualTo("c");

              assertThat(parameterList[1].getType().getCanonicalText()).isEqualTo("int");
              assertThat(parameterList[1].getName()).isEqualTo("number");

              assertThat(onEventMethod.getReturnType().getCanonicalText()).isEqualTo("boolean");
              assertThat(onEventMethod.getName()).isEqualTo("onTestEventWithReturnTypeAndParam");
            });
  }

  @Test
  public void
      createOnEventMethod_whenMethodForGivenEventTypeWithDefaultNameExists_createMethodWithDefaultNameAndPostfix() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass testClassWithOnEventMethod =
                  LithoPluginUtils.getFirstClass(
                          psiFile, cls -> "TestClassWithOnEventMethod".equals(cls.getName()))
                      .get();
              final PsiMethod onEventMethod =
                  OnEventGenerateUtils.createOnEventMethod(
                      testClassWithOnEventMethod, testEvent, Collections.emptyList());
              assertThat(onEventMethod.getName()).isEqualTo("onTestEvent1");
            });
  }

  @Test
  public void
      createOnEventMethod_whenMethodForGivenEventTypeWithCustomNameExists_createMethodWithDefaultName() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass testClassWithOnEventMethod =
                  LithoPluginUtils.getFirstClass(
                          psiFile, cls -> "TestClassWithCustomOnEventMethod".equals(cls.getName()))
                      .get();
              final PsiMethod onEventMethod =
                  OnEventGenerateUtils.createOnEventMethod(
                      testClassWithOnEventMethod, testEvent, Collections.emptyList());
              assertThat(onEventMethod.getName()).isEqualTo("onTestEvent");
            });
  }

  @Test
  public void
      createOnEventMethod_whenMultipleMethodsForGivenEventTypeWithDefaultNamesExist_createMethodWithDefaultNameAndSmallestAvailablePostfix() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass testClassWithMultipleOnEventMethods =
                  LithoPluginUtils.getFirstClass(
                          psiFile,
                          cls -> "TestClassWithMultipleOnEventMethods".equals(cls.getName()))
                      .get();
              final PsiMethod onEventMethod =
                  OnEventGenerateUtils.createOnEventMethod(
                      testClassWithMultipleOnEventMethods, testEvent, Collections.emptyList());
              assertThat(onEventMethod.getName()).isEqualTo("onTestEvent2");
            });
  }
}
