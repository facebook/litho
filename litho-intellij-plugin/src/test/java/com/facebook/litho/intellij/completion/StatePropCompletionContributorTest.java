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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.facebook.litho.intellij.LithoPluginTestHelper;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StatePropCompletionContributorTest {

  private final LithoPluginTestHelper testHelper = new LithoPluginTestHelper("testdata/completion");
  private JavaCodeInsightTestFixture fixture;

  @Before
  public void setUp() throws Exception {
    // Reset this property to prevent test failure on CI. More context in D14854416
    final Properties props = System.getProperties();
    props.setProperty("javax.accessibility.assistive_technologies", "");

    final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder =
        IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("test");
    fixture =
        JavaTestFixtureFactory.getFixtureFactory()
            .createCodeInsightFixture(projectBuilder.getFixture());
    fixture.setUp();
  }

  @After
  public void tearDown() throws Exception {
    fixture.tearDown();
  }

  @Test
  public void testPropCompletion() throws IOException {
    String clsName = "PropCompletionTest.java";

    fixture.configureByText(clsName, testHelper.getContent(clsName));
    fixture.complete(CompletionType.BASIC);
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);
    assertEquals(2, completion.size());
    assertTrue(completion.containsAll(Arrays.asList("int", "iany prop")));
  }

  @Test
  public void testStateCompletion() throws IOException {
    String clsName = "StateCompletionTest.java";

    fixture.configureByText(clsName, testHelper.getContent(clsName));
    fixture.complete(CompletionType.BASIC);
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);
    assertEquals(2, completion.size());
    assertTrue(completion.containsAll(Arrays.asList("int", "irandom state")));
  }
}
