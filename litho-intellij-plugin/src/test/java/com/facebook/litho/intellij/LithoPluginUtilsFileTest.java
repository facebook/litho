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

package com.facebook.litho.intellij;

import static org.assertj.core.api.Assertions.assertThat;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;

public class LithoPluginUtilsFileTest extends LithoPluginIntellijTest {

  public LithoPluginUtilsFileTest() {
    super("testdata/file");
  }

  @Test
  public void getFirstClass_findNestedChildWithName_found() throws IOException {
    final PsiFile file = testHelper.configure("LithoPluginUtilsTest.java");
    final AtomicReference<PsiClass> found = new AtomicReference<>();
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              found.set(
                  LithoPluginUtils.getFirstClass(file, cls -> "InnerClass2".equals(cls.getName()))
                      .orElse(null));
            });
    assertThat(found.get()).isNotNull();
  }

  @Test
  public void getFirstLayoutSpec_findDirectChild_found() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass layoutCls = psiClasses.get(0);
          PsiClass otherCls = psiClasses.get(1);

          Assert.assertTrue(
              LithoPluginUtils.getFirstLayoutSpec(layoutCls.getContainingFile()).isPresent());
          Assert.assertFalse(
              LithoPluginUtils.getFirstLayoutSpec(otherCls.getContainingFile()).isPresent());
          return true;
        },
        "LayoutSpec.java",
        "MountSpec.java");
  }

  @Test
  public void isLayoutSpec() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass layoutCls = psiClasses.get(0);
          PsiClass otherCls = psiClasses.get(1);

          Assert.assertTrue(LithoPluginUtils.isLayoutSpec(layoutCls));
          Assert.assertFalse(LithoPluginUtils.isLayoutSpec(otherCls));
          return true;
        },
        "LayoutSpec.java",
        "MountSpec.java");
  }
}
