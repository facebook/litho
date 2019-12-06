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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

public class LayoutSpecAnnotationsContributorTest extends LithoPluginIntellijTest {

  public LayoutSpecAnnotationsContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void annotationInLayoutSpecCompletion() throws IOException {
    String clsName = "LayoutSpecAnnotationsContributorSpec.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);

    for (String name : LayoutSpecMethodAnnotationsProvider.ANNOTATION_QUALIFIED_NAMES) {
      assertTrue(completion.contains(LithoClassNames.shortName(name)));
    }
  }

  @Test
  public void annotationNotInLayoutSpecCompletion() throws IOException {
    String clsName = "NotLayoutSpecAnnotationsContributor.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    fixture.completeBasic();
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);

    for (String name : LayoutSpecMethodAnnotationsProvider.ANNOTATION_QUALIFIED_NAMES) {
      assertFalse(completion.contains(LithoClassNames.shortName(name)));
    }
  }
}
