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

package com.facebook.litho.intellij.inspections;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import org.junit.Test;
import org.mockito.Mockito;

public class OnEventCreateFixTest extends LithoPluginIntellijTest {

  public OnEventCreateFixTest() {
    super("testdata/inspections");
  }

  @Test
  public void invoke() {
    testHelper.getPsiClass(
        classes -> {
          final CodeInsightTestFixture fixture = testHelper.getFixture();
          final PsiMethodCallExpression call =
              PsiTreeUtil.findChildOfType(classes.get(0), PsiMethodCallExpression.class);
          final LightVirtualFile virtualFile = createVirtualFile(classes.get(0));
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

          AddArgumentFix.createNewMethodCallFix(
                  call, "TestName", eventClass, Mockito.mock(PsiClass.class))
              .invoke(fixture.getProject(), fixture.getEditor(), fixture.getFile());

          assertThat(fixture.getEditor().getDocument().getText()).contains(generatedOnEvent);
          assertThat(call.getArgumentList().getExpressions()[0].getText())
              .isEqualTo("TestName.onTestClass()");
          return true;
        },
        "EventHandlerAnnotatorSpec.java");
  }

  private static LightVirtualFile createVirtualFile(PsiClass cls) {
    final PsiFile psiFile = cls.getContainingFile();
    return new LightVirtualFile(psiFile.getName(), psiFile.getFileType(), psiFile.getText());
  }
}
