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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertSame;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.junit.Test;

public class ComponentsCacheServiceTest extends LithoPluginIntellijTest {

  public ComponentsCacheServiceTest() {
    super("testdata/services");
  }

  @Test
  public void getComponentAndMaybeUpdate() {
    final Project project = testHelper.getFixture().getProject();
    final ComponentsCacheService service =
        ServiceManager.getService(project, ComponentsCacheService.class);
    testHelper.getPsiClass(
        psiClasses -> {
          assertNotNull(psiClasses);
          PsiClass layoutCls = psiClasses.get(0);
          final PsiClass component1 = service.getComponentAndMaybeUpdate(layoutCls, false);

          assertNotNull(component1);
          assertThat(component1.getName()).isEqualTo("Layout");

          // Check value is cached
          final PsiClass component2 = service.getComponentAndMaybeUpdate(layoutCls, false);
          assertSame(component1, component2);
          return true;
        },
        "LayoutSpec.java");
  }
}
