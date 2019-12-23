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

import static org.junit.Assert.assertEquals;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Collection;
import java.util.function.Function;
import org.junit.Test;

public class RequiredPropAnnotatorTest extends LithoPluginIntellijTest {

  public RequiredPropAnnotatorTest() {
    super("testdata/inspections");
  }

  @Test
  public void annotateStatement() {
    testHelper.getPsiClass(
        psiClasses -> {
          assertEquals(2, psiClasses.size());

          PsiClass underTest = psiClasses.get(0);
          PsiClass component = psiClasses.get(1);

          // For testing environment
          Function<PsiMethodCallExpression, PsiClass> resolver =
              psiMethodCallExpression -> component;
          RequiredPropAnnotator annotator = new RequiredPropAnnotator(resolver);
          TestHolder holder = new TestHolder();
          Collection<PsiStatement> statements =
              PsiTreeUtil.findChildrenOfAnyType(underTest, PsiStatement.class);
          // Simulates IDE behavior of traversing Psi elements
          for (PsiStatement statement : statements) {
            annotator.annotate(statement, holder);
          }

          assertEquals(3, holder.errorMessages.size());
          for (String errorMessage : holder.errorMessages) {
            assertEquals(
                "The following props are not "
                    + "marked as optional and were not supplied: testRequiredPropName",
                errorMessage);
          }
          assertEquals(3, holder.errorElements.size());
          for (PsiElement errorElement : holder.errorElements) {
            assertEquals("RequiredPropAnnotatorComponent.create", errorElement.getText());
          }
          return true;
        },
        "RequiredPropAnnotatorTest.java",
        "RequiredPropAnnotatorComponent.java");
  }
}
