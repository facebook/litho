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

package com.facebook.litho.intellij.inspections;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class SpecAnnotatorTest extends LithoPluginIntellijTest {
  private TestHolder holder;
  private SpecAnnotator specAnnotator;

  public SpecAnnotatorTest() {
    super("testdata/inspections");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    holder = new TestHolder();
    specAnnotator = new SpecAnnotator();
  }

  @Test
  public void annotate_forFileWithLayoutSpec_showsErrorMessages() throws IOException {
    final PsiFile file = testHelper.configure("LayoutSpecAnnotatorSpec.java");
    ApplicationManager.getApplication().invokeAndWait(() -> specAnnotator.annotate(file, holder));
    assertForLayoutSpecShowsErrorMessages(holder);
  }

  @Test
  public void annotate_forLayoutSpec_showsErrorMessages() throws IOException {
    final PsiFile file = testHelper.configure("LayoutSpecAnnotatorSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass cls = LithoPluginUtils.getFirstLayoutSpec(file).get();
              specAnnotator.annotate(cls, holder);
            });
    assertForLayoutSpecShowsErrorMessages(holder);
  }

  private static void assertForLayoutSpecShowsErrorMessages(TestHolder holder) {
    assertThat(holder.errorMessages).hasSize(1);
    assertThat(holder.errorMessages)
        .containsExactly(
            "You need to have a method annotated with either @OnCreateLayout or @OnCreateLayoutWithSizeSpec in your spec. In most cases, @OnCreateLayout is what you want.");
  }

  @Test
  public void annotate_forFileWithMountSpec_showsErrorMessages() throws IOException {
    final PsiFile file = testHelper.configure("MountSpecAnnotatorSpec.java");
    ApplicationManager.getApplication().invokeAndWait(() -> specAnnotator.annotate(file, holder));
    assertForMountSpecShowsErrorMessages(holder);
  }

  @Test
  public void annotate_forMountSpec_showsErrorMessages() throws IOException {
    final PsiFile file = testHelper.configure("MountSpecAnnotatorSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass cls =
                  LithoPluginUtils.getFirstClass(file, LithoPluginUtils::isMountSpec).get();
              specAnnotator.annotate(cls, holder);
            });
    assertForMountSpecShowsErrorMessages(holder);
  }

  private static void assertForMountSpecShowsErrorMessages(TestHolder holder) {
    assertThat(holder.errorMessages).hasSize(3);
    assertThat(holder.errorMessages)
        .containsExactly(
            "All MountSpecs need to have a method annotated with @OnCreateMountContent.",
            "onCreateMountContent's return type should be either a View or a Drawable subclass.",
            "MountSpecAnnotatorSpec does not define @OnCreateMountContent method which is required for all @MountSpecs.");
  }
}
