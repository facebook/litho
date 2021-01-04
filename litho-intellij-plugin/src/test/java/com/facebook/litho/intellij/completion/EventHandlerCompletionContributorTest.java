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
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.facebook.litho.specmodels.model.SpecModel;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class EventHandlerCompletionContributorTest extends LithoPluginIntellijTest {

  public EventHandlerCompletionContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void
      EventHandlerCompletionContributor_whenTriggeringCompletionWithEventName_showsEventHandlersFromGeneratedComponent()
          throws IOException {
    PsiFile psiFile = testHelper.configure("EventHandlerCompletionContributorTest.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              PsiClass psiClass = LithoPluginUtils.getFirstLayoutSpec(psiFile).get();
              ComponentGenerateService.getInstance().updateComponentSync(psiClass);
              SpecModel test = ComponentGenerateService.getInstance().getSpecModel(psiClass);
              CodeInsightTestFixture fixture = testHelper.getFixture();
              fixture.complete(CompletionType.BASIC);
              List<String> completion = fixture.getLookupElementStrings();
              assertThat(completion).isNotNull();
              assertThat(completion).hasSize(6);
              assertThat(completion)
                  .containsAll(
                      Arrays.asList(
                          "EventHandlerAnnotator.handler1()",
                          "EventHandlerAnnotator.handlerTwo()",
                          "EventHandlerAnnotator.thirdHandler()"));
            });
  }

  @Test
  public void
      EventHandlerCompletionContributor_whenTriggeringCompletionWithMethodName_showsEventHandlersFromGeneratedComponent()
          throws IOException {
    testHelper.configure("EventHandlerCompletionContributorTest2.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              CodeInsightTestFixture fixture = testHelper.getFixture();
              fixture.complete(CompletionType.BASIC);
              List<String> completion = fixture.getLookupElementStrings();
              assertThat(completion).isNotNull();
              assertThat(completion).hasSize(3);
              assertThat(completion)
                  .containsAll(
                      Arrays.asList(
                          "EventHandlerAnnotator.handler1()",
                          "EventHandlerAnnotator.handlerTwo()",
                          "EventHandlerAnnotator.thirdHandler()"));
            });
  }
}
