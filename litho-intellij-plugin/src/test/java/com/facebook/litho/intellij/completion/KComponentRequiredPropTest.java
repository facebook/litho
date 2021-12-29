/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.junit.Test;

public class KComponentRequiredPropTest extends LithoPluginIntellijTest {

  public KComponentRequiredPropTest() {
    super("testdata/completion");
  }

  @Test
  public void LithoKotlinWrapperRequiredPropLookupStringCreationTest() throws IOException {
    final String clsName = "LithoKotlinWrapperTestFunction.kt";
    testHelper.configure(clsName);
    final CodeInsightTestFixture fixture = testHelper.getFixture();
    final String[] lookupString = new String[1];
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              KtNamedFunction lithoKotlinFunction =
                  PsiTreeUtil.getChildOfType(fixture.getFile(), KtNamedFunction.class);
              lookupString[0] =
                  KComponentRequiredPropMethodContributor.createKotlinCompletionString(
                      lithoKotlinFunction);
            });
    assertThat(lookupString[0]).contains("options = , selectedOption = , onItemSelected = ");
  }

  @Test
  public void KComponentRequiredPropLookupStringCreationTest() throws IOException {
    final String clsName = "KcomponentTestFunction.kt";
    testHelper.configure(clsName);
    final CodeInsightTestFixture fixture = testHelper.getFixture();
    final String[] lookupString = new String[1];
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              KtClass lithoKotlinClass =
                  PsiTreeUtil.getChildOfType(fixture.getFile(), KtClass.class);
              lookupString[0] =
                  KComponentRequiredPropMethodContributor.createKotlinCompletionStringForKComponent(
                      lithoKotlinClass);
            });
    assertThat(lookupString[0]).contains("text = , selected = ) {}");
  }
}
