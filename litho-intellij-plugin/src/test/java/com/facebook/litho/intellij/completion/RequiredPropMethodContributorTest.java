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
import static org.mockito.Mockito.mock;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.TestLookupElementPresentation;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

public class RequiredPropMethodContributorTest extends LithoPluginIntellijTest {

  public RequiredPropMethodContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void handleInsert() throws IOException {
    String clsName = "RequiredPropMethodContributorTest.java";
    // Setup caret
    testHelper.configure(clsName);
    testHelper.getPsiClass(
        psiClasses -> {
          final PsiClass componentCls = getComponent(psiClasses);
          final PsiMethod createMethod = componentCls.getMethods()[0];
          final LookupElement testDelegate = new MethodChainLookupElementTest.TestLookupElement("");

          final CodeInsightTestFixture fixture = testHelper.getFixture();
          final int endOffset = fixture.getCaretOffset();
          final int startOffsetBeforeDot = endOffset - 10;
          final PsiElement placeholder = psiClasses.get(0).findElementAt(endOffset).getParent();
          final Project project = fixture.getProject();
          final Editor editor = fixture.getEditor();
          final Document document = editor.getDocument();

          final MethodChainLookupElement methodLookup =
              (MethodChainLookupElement)
                  RequiredPropMethodContributor.RequiredPropMethodProvider.createMethodLookup(
                          createMethod, testDelegate, placeholder, project)
                      .get();
          WriteCommandAction.runWriteCommandAction(
              project,
              () -> {
                methodLookup.handleInsertInternal(
                    editor, document, startOffsetBeforeDot, endOffset, project);
              });

          assertThat(document.getText())
              .contains("MyComponent.create(c)\n        .one()\n        .three()");

          return true;
        },
        clsName);
  }

  @Test
  public void renderElement_addTail() {
    LookupElementPresentation testPresentation = new TestLookupElementPresentation();

    // Excluding input parameters influence on the render result
    RequiredPropLookupElement.create(mock(LookupElement.class), true)
        .renderElement(testPresentation);

    assertThat(testPresentation.getTailText()).isEqualTo(" - required Prop");
  }

  @Test
  public void isComponentCreateMethod() {
    testHelper.getPsiClass(
        psiClasses -> {
          final PsiClass componentCls = getComponent(psiClasses);
          final PsiMethod[] methods = componentCls.getMethods();

          assertThat(
                  RequiredPropMethodContributor.RequiredPropMethodProvider.isComponentCreateMethod(
                      methods[0]))
              .isTrue();
          assertThat(
                  RequiredPropMethodContributor.RequiredPropMethodProvider.isComponentCreateMethod(
                      methods[1]))
              .isFalse();

          return true;
        },
        "RequiredPropMethodContributorTest.java");
  }

  @Test
  public void findRequiredPropSetterNames() {
    testHelper.getPsiClass(
        psiClasses -> {
          final PsiClass componentCls = getComponent(psiClasses);
          String[] expected = {"one", "three"};

          List<String> names =
              RequiredPropMethodContributor.RequiredPropMethodProvider.findRequiredPropSetterNames(
                  componentCls);
          assertThat(names.size()).isEqualTo(2);
          assertThat(names.toArray()).isEqualTo(expected);
          return true;
        },
        "RequiredPropMethodContributorTest.java");
  }

  private static PsiClass getComponent(List<PsiClass> psiClasses) {
    PsiClass cls = psiClasses.get(0);
    return cls.findInnerClassByName("MyComponent", false);
  }
}
