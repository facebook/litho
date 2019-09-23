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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class StatePropCompletionContributorTest extends LithoPluginIntellijTest {

  public StatePropCompletionContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void testPropCompletion() throws IOException {
    String clsName = "PropCompletionSpec.java";
    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.complete(CompletionType.BASIC);
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);
    assertEquals(2, completion.size());
    assertTrue(completion.containsAll(Arrays.asList("int", "iany prop")));
  }

  @Test
  public void testStateCompletion() throws IOException {
    String clsName = "StateCompletionSpec.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.complete(CompletionType.BASIC);
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);
    assertEquals(2, completion.size());
    assertTrue(completion.containsAll(Arrays.asList("int", "irandom state")));
  }

  @Test
  public void testNotSpecCompletion() throws IOException {
    String clsName = "NotSpecCompletion.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.complete(CompletionType.BASIC);
    List<String> completion = fixture.getLookupElementStrings();
    assertNull(completion);
  }
}
