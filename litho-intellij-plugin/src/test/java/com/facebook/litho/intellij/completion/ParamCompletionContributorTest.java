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
import static org.assertj.core.api.Java6Assertions.atIndex;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import org.junit.Test;

public class ParamCompletionContributorTest extends LithoPluginIntellijTest {

  public ParamCompletionContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void whenTriggeringCompletionForNotAllowedMethod_doesNotShowParamAnnotation()
      throws IOException {
    testHelper.configure("ParamCompletionNotAllowedMethodTest.java");
    final CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(fixture.getLookupElementStrings()).doesNotContain("Param");
  }

  @Test
  public void whenTriggeringCompletionForOnEvent_showsParamAnnotationPrioritised()
      throws IOException {
    testHelper.configure("ParamCompletionOnEventTest.java");
    final CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(fixture.getLookupElementStrings()).contains("Param", atIndex(0));
  }

  @Test
  public void whenTriggeringCompletionForOnUpdateState_showsParamAnnotationPrioritised()
      throws IOException {
    testHelper.configure("ParamCompletionOnUpdateStateTest.java");
    final CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(fixture.getLookupElementStrings()).contains("Param", atIndex(0));
  }

  @Test
  public void
      whenTriggeringCompletionForOnUpdateStateWithTransition_showsParamAnnotationPrioritised()
          throws IOException {
    testHelper.configure("ParamCompletionOnUpdateStateWithTransitionTest.java");
    final CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    assertThat(fixture.getLookupElementStrings()).contains("Param", atIndex(0));
  }
}
