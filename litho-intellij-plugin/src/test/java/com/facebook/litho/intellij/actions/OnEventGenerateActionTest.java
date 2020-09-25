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

package com.facebook.litho.intellij.actions;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import org.junit.Before;
import org.junit.Test;

public class OnEventGenerateActionTest extends LithoPluginIntellijTest {
  private PsiFile psiFile;
  private PsiClass testClass;
  private OnEventGenerateAction.EventChooser eventChooser;
  private OnEventGenerateAction.OnEventGeneratedListener onEventGeneratedListener;

  public OnEventGenerateActionTest() {
    super("testdata/completion");
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    psiFile = testHelper.configure("OnEventGenerateActionTest.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              testClass =
                  LithoPluginUtils.getFirstClass(psiFile, cls -> "TestClass".equals(cls.getName()))
                      .get();
            });
    eventChooser = mock(OnEventGenerateAction.EventChooser.class);
  }

  @Test
  public void createHandler_handlerInvoked_documentContainsOnEventMethod() {
    testHelper.getPsiClass(
        classes -> {
          final CodeInsightTestFixture fixture = testHelper.getFixture();
          final PsiFile psiFile = classes.get(0).getContainingFile();
          final LightVirtualFile virtualFile =
              new LightVirtualFile(psiFile.getName(), psiFile.getFileType(), psiFile.getText());
          final String generatedOnEvent =
              "@com.facebook.litho.annotations.OnEvent(TestClass.class)\n"
                  + "    static void onTestClass(com.facebook.litho.ComponentContext c) {\n"
                  + "    }";
          final PsiClass eventClass =
              JavaPsiFacade.getInstance(fixture.getProject())
                  .getElementFactory()
                  .createClass("TestClass");

          fixture.openFileInEditor(virtualFile);

          assertThat(fixture.getEditor().getDocument().getText()).doesNotContain(generatedOnEvent);

          TransactionGuard.getInstance()
              .submitTransactionAndWait(
                  () ->
                      OnEventGenerateAction.createHandler(
                              (context, project) -> eventClass,
                              onEvent -> {
                                assertThat(onEvent.getName()).isEqualTo("onTestClass");
                                assertThat(onEvent.getAnnotations()[0].getQualifiedName())
                                    .isEqualTo("com.facebook.litho.annotations.OnEvent");
                              })
                          .invoke(fixture.getProject(), fixture.getEditor(), fixture.getFile()));

          assertThat(fixture.getEditor().getDocument().getText()).contains(generatedOnEvent);
          return true;
        },
        "LayoutSpec.java");
  }

  @Test
  public void chooseOriginalMembers_eventChosen_returnsInfoForGeneratedMethodInfoForChosenEvent() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass testEvent =
                  LithoPluginUtils.getFirstClass(psiFile, cls -> "TestEvent".equals(cls.getName()))
                      .get();
              when(eventChooser.choose(testClass, testClass.getProject())).thenReturn(testEvent);
              onEventGeneratedListener = mock(OnEventGenerateAction.OnEventGeneratedListener.class);
              final OnEventGenerateAction.OnEventGenerateHandler testHandler =
                  new OnEventGenerateAction.OnEventGenerateHandler(
                      eventChooser, null, onEventGeneratedListener);

              final ClassMember[] classMembers =
                  testHandler.chooseOriginalMembers(testClass, testClass.getProject());
              assertThat(classMembers).hasSize(1);

              final PsiMethod psiMethod = ((PsiMethodMember) classMembers[0]).getElement();
              assertThat(psiMethod.getName()).isEqualTo("onTestEvent");
              assertThat(psiMethod.getReturnType().getCanonicalText()).isEqualTo("void");

              assertThat(psiMethod.getParameterList().getParameters()).hasSize(1);
              assertThat(psiMethod.getParameterList().getParameters()[0].getName()).isEqualTo("c");
              assertThat(
                      psiMethod.getParameterList().getParameters()[0].getType().getCanonicalText())
                  .isEqualTo("com.facebook.litho.ComponentContext");

              assertThat(psiMethod.getAnnotations()).hasSize(1);
              assertThat(psiMethod.getAnnotations()[0].getQualifiedName())
                  .isEqualTo("com.facebook.litho.annotations.OnEvent");
              assertThat(
                      psiMethod.getAnnotations()[0].getParameterList().getAttributes()[0].getText())
                  .isEqualTo("TestEvent.class");
            });
  }

  @Test
  public void chooseOriginalMembers_noEventChosen_returnsEmptyArray() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              onEventGeneratedListener = mock(OnEventGenerateAction.OnEventGeneratedListener.class);
              final OnEventGenerateAction.OnEventGenerateHandler testHandler =
                  new OnEventGenerateAction.OnEventGenerateHandler(
                      eventChooser, null, onEventGeneratedListener);

              assertThat(testHandler.chooseOriginalMembers(testClass, testClass.getProject()))
                  .isEmpty();
            });
  }
}
