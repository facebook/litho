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

package com.facebook.litho.intellij.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;

public class ComponentsMethodDeclarationHandlerTest extends LithoPluginIntellijTest {

  public ComponentsMethodDeclarationHandlerTest() {
    super("testdata/navigation");
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    PsiSearchUtils.clearMocks();
  }

  @Test
  public void getGotoDeclarationTargets_findMethods_found() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiFile file =
                  testHelper
                      .getFixture()
                      .configureByFile("ComponentsMethodDeclarationHandlerTest.java");
              PsiClass testSpecClass = findClass("TestSpec", file);
              final PsiIdentifier componentMethodCall =
                  findIdentifier(testSpecClass.findMethodsByName("test1", false)[0], "method1");

              final PsiElement[] specMethods =
                  new ComponentsMethodDeclarationHandler()
                      .getGotoDeclarationTargets(
                          componentMethodCall, 0, testHelper.getFixture().getEditor());
              assertThat(specMethods.length).isOne();
              PsiMethod targetMethod = (PsiMethod) specMethods[0];
              assertThat(targetMethod.getName()).isEqualTo("method1");
              assertThat(targetMethod.getContainingClass()).isSameAs(testSpecClass);
            });
  }

  @Test
  public void getGotoDeclarationTargets_findProps_found() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiFile file =
                  testHelper
                      .getFixture()
                      .configureByFile("ComponentsMethodDeclarationHandlerTest.java");
              PsiClass testSpecClass = findClass("TestSpec", file);
              final PsiIdentifier componentMethodCall =
                  findIdentifier(testSpecClass.findMethodsByName("test2", false)[0], "prop");

              final PsiElement[] specElements =
                  new ComponentsMethodDeclarationHandler()
                      .getGotoDeclarationTargets(
                          componentMethodCall, 0, testHelper.getFixture().getEditor());
              assertThat(specElements.length).isOne();
              PsiParameter targetProp = (PsiParameter) specElements[0];
              assertThat(targetProp.getName()).isEqualTo("prop");
            });
  }

  @Nullable
  private PsiClass findClass(String name, PsiFile file) {
    for (PsiElement element : file.getChildren()) {
      if (element instanceof PsiClass) {
        final PsiClass cls = (PsiClass) element;
        if (name.equals(cls.getName())) {
          PsiSearchUtils.addMock(name, cls);
          return cls;
        }
      }
    }
    return null;
  }

  private static PsiIdentifier findIdentifier(PsiElement cls, String name) {
    final Collection<PsiIdentifier> identifiers =
        PsiTreeUtil.findChildrenOfType(cls, PsiIdentifier.class);
    for (PsiIdentifier identifier : identifiers) {
      if (identifier.textMatches(name)) return identifier;
    }
    return null;
  }
}
