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

package com.facebook.litho.intellij.file;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.services.ComponentsCacheService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.IOException;
import org.junit.Test;

public class ComponentShortNamesCacheTest extends LithoPluginIntellijTest {

  public ComponentShortNamesCacheTest() {
    super("testdata/file");
  }

  @Test
  public void getAllClassNames() throws IOException {
    // Add file to cache
    final Project project = testHelper.getFixture().getProject();
    final PsiFile file = testHelper.configure("LayoutSpec.java");
    final ComponentShortNamesCache namesCache = new ComponentShortNamesCache(project);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // Add file to cache
              final PsiClass cls = PsiTreeUtil.findChildOfType(file, PsiClass.class);
              ServiceManager.getService(project, ComponentsCacheService.class)
                  .maybeUpdate(cls, false);

              final String[] allClassNames = namesCache.getAllClassNames();
              assertThat(allClassNames.length).isOne();
              assertThat(allClassNames[0]).isEqualTo("Layout");
            });
  }
}
