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
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class OnEventCompletionTest extends LithoPluginIntellijTest {

  public OnEventCompletionTest() {
    super("testdata/completion");
  }

  @Test
  public void eventInLithoClass_completes() throws IOException {
    String clsName = "OnClickEventCompletionSpec.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    @Nullable List<String> lookupElements = fixture.getLookupElementStrings();
    assertThat(lookupElements).isNotNull();
    assertThat(lookupElements).contains("onClickEvent");
  }

  @Test
  public void eventNotInLithoClass_notCompletes() throws IOException {
    String clsName = "OnClickEventNotLithoCompletionTest.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    LookupElement[] lookupElements = fixture.completeBasic();
    assertThat(lookupElements).isNotNull();
    assertThat(lookupElements.length == 0).isTrue();
  }

  @Test
  public void eventAboveMethod_completes() throws IOException {
    String clsName = "OnClickEventAboveMethodCompletionSpec.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    @Nullable List<String> lookupElements = fixture.getLookupElementStrings();
    assertThat(lookupElements).isNotNull();
    assertThat(lookupElements).contains("onClickEvent");
  }
}
