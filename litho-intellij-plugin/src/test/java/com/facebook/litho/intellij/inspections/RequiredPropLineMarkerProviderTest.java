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

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.Test;

public class RequiredPropLineMarkerProviderTest extends LithoPluginIntellijTest {

  public RequiredPropLineMarkerProviderTest() {
    super("testdata/inspections");
  }

  @Test
  public void markStatement() {
    testHelper.getPsiClass(
        psiClasses -> {
          assertThat(psiClasses).hasSize(2);

          PsiClass underTest = psiClasses.get(0);
          PsiClass component = psiClasses.get(1);

          // For testing environment
          Function<PsiMethodCallExpression, PsiClass> resolver = ignored -> component;
          RequiredPropLineMarkerProvider provider = new RequiredPropLineMarkerProvider(resolver);
          List<PsiElement> statements =
              new ArrayList<>(PsiTreeUtil.findChildrenOfAnyType(underTest, PsiStatement.class));

          assertThat(provider.getLineMarkerInfo(statements.get(0))).isNotNull();
          assertThat(provider.getLineMarkerInfo(statements.get(1))).isNull();

          return true;
        },
        "RequiredPropAnnotatorTest.java",
        "RequiredPropAnnotatorComponent.java");
  }
}
