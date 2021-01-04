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

package com.facebook.litho.intellij.redsymbols;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.IOException;
import org.junit.After;
import org.junit.Test;

public class ComponentFinderTest extends LithoPluginIntellijTest {

  public ComponentFinderTest() {
    super("testdata/file");
  }

  @After
  @Override
  public void tearDown() throws Exception {
    ComponentsCacheService.getInstance(testHelper.getProject()).dispose();
    super.tearDown();
  }

  @Test
  public void findClass() throws IOException {
    final Project project = testHelper.getFixture().getProject();
    final PsiFile file = testHelper.configure("LayoutSpec.java");

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final ComponentFinder finder = new ComponentFinder(project);
              // Add file to cache
              final PsiClass cls = PsiTreeUtil.findChildOfType(file, PsiClass.class);
              ComponentGenerateService.getInstance().updateComponentSync(cls);

              // No result with project scope
              final PsiClass result1 =
                  finder.findClass("Layout", GlobalSearchScope.projectScope(project));
              assertThat(result1).isNull();

              // Result with component scope
              final PsiClass result2 = finder.findClass("Layout", ComponentScope.getInstance());
              assertThat(result2).isNotNull();
            });
  }
}
