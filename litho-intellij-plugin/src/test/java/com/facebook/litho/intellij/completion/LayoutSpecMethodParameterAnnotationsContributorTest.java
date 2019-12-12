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

import static junit.framework.Assert.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class LayoutSpecMethodParameterAnnotationsContributorTest extends LithoPluginIntellijTest {

  public LayoutSpecMethodParameterAnnotationsContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void addCompletions_inLayoutSpec() throws IOException {
    String clsName = "LayoutSpecMethodParameterAnnotationsContributorSpec.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);

    Set<String> params =
        LayoutSpecMethodParameterAnnotationsContributor.Provider
            .LAYOUT_SPEC_DELEGATE_METHOD_TO_PARAMETER_ANNOTATIONS
            .get("com.facebook.litho.annotations.OnCreateLayout");
    assertThat(completion).hasSize(params.size());

    for (String name : params) {
      assertThat(completion).contains(LithoClassNames.shortName(name));
    }
  }

  @Test
  public void addCompletions_notInLayoutSpec() throws IOException {
    String clsName = "NotLayoutSpecMethodParameterAnnotationsContributorSpec.java";

    testHelper.configure(clsName);
    CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    List<String> completion = fixture.getLookupElementStrings();
    assertNotNull(completion);
    assertThat(completion).isEmpty();
  }
}
