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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.Test;

public class AddArgumentFixTest extends LithoPluginIntellijTest {

  public AddArgumentFixTest() {
    super("testdata/inspections");
  }

  @Test
  public void createAddMethodCallFix() {
    testHelper.getPsiClass(
        classes -> {
          // Setup test environment
          PsiClass cls = classes.get(0);
          PsiMethodCallExpression call =
              PsiTreeUtil.findChildOfType(cls, PsiMethodCallExpression.class);
          Project project = testHelper.getFixture().getProject();
          PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
          Editor editor = mock(Editor.class);
          when(editor.getCaretModel()).thenReturn(mock(CaretModel.class));

          IntentionAction fix =
              AddArgumentFix.createAddMethodCallFix(call, "ClassName", "methodName", factory);

          assertThat(call.getArgumentList().getExpressions()[0].getText())
              .isNotEqualTo("ClassName.methodName()");
          fix.invoke(project, editor, testHelper.getFixture().getFile());
          assertThat(call.getArgumentList().getExpressions()[0].getText())
              .isEqualTo("ClassName.methodName()");
          return true;
        },
        "RequiredPropAnnotatorTest.java");
  }
}
