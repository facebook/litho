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

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import org.junit.Test;

public class FromParameterProviderTest extends LithoPluginIntellijTest {

  public FromParameterProviderTest() {
    super("testdata/completion");
  }

  @Test
  public void triggeringCompletionForFromEvent() throws IOException {
    String clsName = "FromParameterProviderFromEventTest.java";

    PsiFile layoutSpec = testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(layoutSpec.getText().contains("@FromEvent boolean someValue")).isTrue();
  }

  @Test
  public void triggeringCompletionForFromTrigger() throws IOException {
    String clsName = "FromParameterProviderFromTriggerTest.java";

    PsiFile layoutSpec = testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(layoutSpec.getText().contains("@FromTrigger boolean someValue")).isTrue();
  }

  @Test
  public void triggeringCompletionForNotAllowedAnnotation() throws IOException {
    String clsName = "FromParameterProviderNotAllowedAnnotationTest.java";

    PsiFile layoutSpec = testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(layoutSpec.getText().contains("@FromEvent")).isFalse();
  }
}
