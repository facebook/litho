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
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.List;
import org.junit.Test;

public class EventHandlerAnnotatorTest extends LithoPluginIntellijTest {

  public EventHandlerAnnotatorTest() {
    super("testdata/inspections");
  }

  @Test
  public void annotate_added() {
    testHelper.getPsiClass(
        classes -> {
          PsiClass cls = classes.get(0);
          ComponentGenerateService.getInstance(testHelper.getProject())
              .updateLayoutComponentSync(cls);

          PsiMethodCallExpression call =
              PsiTreeUtil.findChildOfType(cls, PsiMethodCallExpression.class);
          assertThat(call.getText()).isEqualTo("Component.create(c).testEventHandler()");

          EventHandlerAnnotator eventHandlerAnnotator = new EventHandlerAnnotator();

          TestHolder holder1 = new TestHolder();
          eventHandlerAnnotator.annotate(call, holder1);

          assertThat(holder1.errorElements).hasSize(1);
          assertThat(holder1.errorElements.get(0).getText()).isEqualTo("()");

          assertThat(holder1.errorMessages).hasSize(1);
          assertThat(holder1.errorMessages.get(0)).isEqualTo("Add TestEventHandler");

          assertThat(holder1.createdAnnotations).hasSize(1);

          List<Annotation.QuickFixInfo> fixes = holder1.createdAnnotations.get(0).getQuickFixes();
          assertThat(fixes).hasSize(3);

          Project project = cls.getProject();
          PsiFile file = cls.getContainingFile();
          Editor editor = mock(Editor.class);
          when(editor.getCaretModel()).thenReturn(mock(CaretModel.class));

          Annotation.QuickFixInfo fix1 = fixes.get(0);
          assertThat(fix1.toString()).isEqualTo("Add .handler1() TestEventHandler");
          fix1.quickFix.invoke(project, editor, file);
          assertThat(call.getArgumentList().getExpressions()[0].getText())
              .isEqualTo("EventHandlerAnnotator.handler1()");

          Annotation.QuickFixInfo fix2 = fixes.get(1);
          assertThat(fix2.toString()).isEqualTo("Add .handlerTwo() TestEventHandler");
          fix2.quickFix.invoke(project, editor, file);
          assertThat(call.getArgumentList().getExpressions()[0].getText())
              .isEqualTo("EventHandlerAnnotator.handlerTwo()");

          Annotation.QuickFixInfo fix3 = fixes.get(2);
          assertThat(fix3.toString()).isEqualTo("Add .thirdHandler() TestEventHandler");
          fix3.quickFix.invoke(project, editor, file);
          assertThat(call.getArgumentList().getExpressions()[0].getText())
              .isEqualTo("EventHandlerAnnotator.thirdHandler()");

          TestHolder holder2 = new TestHolder();
          eventHandlerAnnotator.annotate(call, holder2);
          assertThat(holder2.createdAnnotations).hasSize(0);
          return true;
        },
        "EventHandlerAnnotatorSpec.java");
  }
}
