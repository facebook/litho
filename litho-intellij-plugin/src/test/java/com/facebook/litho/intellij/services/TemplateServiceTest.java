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

package com.facebook.litho.intellij.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.junit.Test;

public class TemplateServiceTest extends LithoPluginIntellijTest {

  public TemplateServiceTest() {
    super("testdata/services");
  }

  @Test
  public void getMethodTemplate() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final Project project = testHelper.getFixture().getProject();
              final PsiMethod onAttached =
                  ServiceManager.getService(project, TemplateService.class)
                      .getMethodTemplate("OnAttached", project);
              assertThat(onAttached).isNotNull();
            });
  }
}
