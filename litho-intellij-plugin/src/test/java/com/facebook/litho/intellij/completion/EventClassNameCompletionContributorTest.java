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
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class EventClassNameCompletionContributorTest extends LithoPluginIntellijTest {

  public EventClassNameCompletionContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void triggeringCompletionForOnEvent() throws IOException {
    String clsName = "EventClassNameCompletionOnEventTest.java";

    PsiFile layoutSpec = testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(layoutSpec.getText().contains("ClickEvent.class")).isTrue();
  }

  @Test
  public void triggeringCompletionForReturnVoidMethod() throws IOException {
    String clsName = "EventClassNameCompletionReturnVoidMethodTest.java";

    PsiFile layoutSpec = testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(layoutSpec.getText().contains("SomeNotEvent")).isTrue();
  }

  @Test
  public void triggeringCompletionForOnTrigger() throws IOException {
    String clsName = "EventClassNameCompletionOnTriggerTest.java";

    PsiFile layoutSpec = testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(layoutSpec.getText().contains("ClickEvent.class")).isTrue();
  }

  @Test
  public void triggeringCompletionForNotAllowedAnnotation() throws IOException {
    String clsName = "EventClassNameCompletionNotAllowedAnnotationTest.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    @Nullable List<String> lookupElements = fixture.getLookupElementStrings();
    assertThat(lookupElements).isNotNull();
    assertThat(lookupElements).contains("SomeNotEvent");
  }

  @Test
  public void triggeringCompletionForNotAllowedClass() throws IOException {
    String clsName = "EventClassNameCompletionNotAllowedClass.java";

    PsiFile layoutSpec = testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(layoutSpec.getText().contains("SomeNotEvent.class")).isFalse();
  }
}
