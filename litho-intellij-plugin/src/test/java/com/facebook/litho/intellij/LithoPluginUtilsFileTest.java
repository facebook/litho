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

import static com.facebook.litho.intellij.LithoPluginUtils.getFirstClass;
import static com.facebook.litho.intellij.LithoPluginUtils.getFirstLayoutSpec;
import static com.facebook.litho.intellij.LithoPluginUtils.isLayoutSpec;
import static org.assertj.core.api.Assertions.assertThat;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.io.IOException;
import java.util.Optional;
import org.junit.Test;

public class LithoPluginUtilsFileTest extends LithoPluginIntellijTest {
  PsiFile psiFile;
  PsiClass layoutSpec;

  public LithoPluginUtilsFileTest() {
    super("testdata/file");
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    psiFile = testHelper.configure("LithoPluginUtilsTest.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              layoutSpec = getFirstLayoutSpec(psiFile).get();
            });
  }

  @Test
  public void getFirstClass_whenLookingForNestedClassByName_returnsFoundClass() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass innerClass2 =
                  getFirstClass(psiFile, cls -> "InnerClass2".equals(cls.getName())).orElse(null);
              assertThat(innerClass2).isNotNull();
            });
  }

  @Test
  public void getFirstLayoutSpec_whenLayoutSpecExists_returnsFoundLayoutSpec() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(getFirstLayoutSpec(psiFile).get()).isNotNull();
              assertThat(getFirstLayoutSpec(psiFile).get().getQualifiedName())
                  .isEqualTo("LithoActivitySpec");
            });
  }

  @Test
  public void getFirstLayoutSpec_whenLayoutSpecDoesNotExist_returnsEmptyResult()
      throws IOException {
    final PsiFile mountSpecFile = testHelper.configure("MountSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(getFirstLayoutSpec(mountSpecFile)).isEqualTo(Optional.empty());
            });
  }

  @Test
  public void isLayoutSpec_whenClassIsNotLayoutSpec_returnsFalse() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass mountSpec =
                  getFirstClass(psiFile, cls -> "MountSpec".equals(cls.getName())).get();
              assertThat(mountSpec)
                  .describedAs("Test file should contain MountSpec class")
                  .isNotNull();
              assertThat(isLayoutSpec(mountSpec)).isFalse();
            });
  }

  @Test
  public void isLayoutSpec_whenClassIsLayoutSpec_returnsTrue() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(isLayoutSpec(layoutSpec)).isTrue();
            });
  }
}
