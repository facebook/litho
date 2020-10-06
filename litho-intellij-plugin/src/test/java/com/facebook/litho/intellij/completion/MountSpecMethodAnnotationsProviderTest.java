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

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.services.TemplateService;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

public class MountSpecMethodAnnotationsProviderTest extends LithoPluginIntellijTest {

  public MountSpecMethodAnnotationsProviderTest() {
    super("testdata/completion");
  }

  @Test
  public void addCompletions_inMountSpec_completes() throws IOException {
    final String clsName = "MountSpecAnnotationsContributorSpec.java";

    testHelper.configure(clsName);
    final CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    final List<String> completion = fixture.getLookupElementStrings();

    final Project project = fixture.getProject();
    for (String name : MountSpecMethodAnnotationsProvider.ANNOTATION_QUALIFIED_NAMES) {
      final String shortName = LithoClassNames.shortName(name);
      if (ServiceManager.getService(project, TemplateService.class)
              .getMethodTemplate(shortName, project)
          != null) {
        continue;
      }

      assertThat(completion.contains(shortName)).describedAs("Doesn't contain %s", name).isTrue();
    }
  }

  @Test
  public void addCompletions_notInMountSpec_notCompletes() throws IOException {
    final String clsName = "NotMountSpecAnnotationsContributor.java";

    testHelper.configure(clsName);
    final CodeInsightTestFixture fixture = testHelper.getFixture();
    fixture.completeBasic();
    fixture.completeBasic();
    final List<String> completion = fixture.getLookupElementStrings();

    for (String name : MountSpecMethodAnnotationsProvider.ANNOTATION_QUALIFIED_NAMES) {
      assertThat(completion.contains(LithoClassNames.shortName(name))).isFalse();
    }
  }
}
