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
import org.junit.Test;

public class LayoutSpecAnnotatorTest extends LithoPluginIntellijTest {

  public LayoutSpecAnnotatorTest() {
    super("testdata/inspections");
  }

  @Test
  public void annotate_file_found() throws IOException {
    final PsiFile file = testHelper.configure("LayoutSpecAnnotatorSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final TestHolder holder = new TestHolder();
              new LayoutSpecAnnotator().annotate(file, holder);
              assertThat(holder.errorMessages).hasSize(2);
              assertThat(holder.errorMessages)
                  .containsExactly(
                      "Methods annotated with interface com.facebook.litho.annotations.OnCreateInitialState must have at least 1 parameters, and they should be of type com.facebook.litho.ComponentContext.",
                      "You need to have a method annotated with either @OnCreateLayout or @OnCreateLayoutWithSizeSpec in your spec. In most cases, @OnCreateLayout is what you want.");
            });
  }

  @Test
  public void annotate_class_found() throws IOException {
    final PsiFile file = testHelper.configure("LayoutSpecAnnotatorSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass cls = LithoPluginUtils.getFirstLayoutSpec(file).get();
              final TestHolder holder = new TestHolder();
              new LayoutSpecAnnotator().annotate(cls, holder);
              assertThat(holder.errorMessages).hasSize(2);
              assertThat(holder.errorMessages)
                  .containsExactly(
                      "Methods annotated with interface com.facebook.litho.annotations.OnCreateInitialState must have at least 1 parameters, and they should be of type com.facebook.litho.ComponentContext.",
                      "You need to have a method annotated with either @OnCreateLayout or @OnCreateLayoutWithSizeSpec in your spec. In most cases, @OnCreateLayout is what you want.");
            });
  }
}
