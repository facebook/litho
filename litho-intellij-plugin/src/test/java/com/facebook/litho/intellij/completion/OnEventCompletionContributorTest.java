/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.intellij.completion;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

public class OnEventCompletionContributorTest extends LithoPluginIntellijTest {

  public OnEventCompletionContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void testEventInLithoClassCompletion() throws IOException {
    String clsName = "OnClickEventCompletionSpec.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    fixture.completeBasic();
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);
    assertTrue(completion.contains("onClickEvent"));
  }

  @Test
  public void testEventNotInLithoClassCompletion() throws IOException {
    String clsName = "OnClickEventNotLithoCompletionTest.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    fixture.completeBasic();
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);
    assertTrue(completion.isEmpty());
  }

  @Test
  public void aboveMethodCompletion() throws IOException {
    testHelper.configure("OnClickEventAboveMethodCompletionSpec.java");
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    fixture.completeBasic();
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);
    assertTrue(completion.contains("onClickEvent"));
  }
}
