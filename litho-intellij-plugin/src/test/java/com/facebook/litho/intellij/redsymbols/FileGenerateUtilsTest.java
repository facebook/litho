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

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.io.IOException;
import java.util.function.BiConsumer;
import org.junit.Ignore;
import org.junit.Test;

public class FileGenerateUtilsTest extends LithoPluginIntellijTest {

  public FileGenerateUtilsTest() {
    super("testdata/services");
  }

  @Ignore("T73932936")
  @Test
  public void updateLayoutComponentAsync_specNotChanged_sameInMemoryComponent() throws IOException {
    retrieveInitialAndUpdatedComponents(
        "InitialLayoutSpec.java",
        "UpdatedLayoutSpecWithSameInterface.java",
        (initialComponent, updatedComponent) ->
            assertThat(updatedComponent)
                .describedAs(
                    "Expected: %s, Got: %s", initialComponent.getText(), updatedComponent.getText())
                .isSameAs(initialComponent));
  }

  @Ignore("T73932936")
  @Test
  public void updateMountComponentAsync_specNotChanged_sameInMemoryComponent() throws IOException {
    retrieveInitialAndUpdatedComponents(
        "InitialMountSpec.java",
        "UpdatedMountSpecWithSameInterface.java",
        (initialComponent, updatedComponent) ->
            assertThat(updatedComponent).isSameAs(initialComponent));
  }

  @Ignore("T73932936")
  @Test
  public void updateLayoutComponentAsync_specChanged_notSameInMemoryComponent() throws IOException {
    retrieveInitialAndUpdatedComponents(
        "InitialLayoutSpec.java",
        "UpdatedLayoutSpecWithDifferentInterface.java",
        (initialComponent, updatedComponent) ->
            assertThat(updatedComponent).isNotSameAs(initialComponent));
  }

  @Ignore("T73932936")
  @Test
  public void updateMountComponentAsync_specChanged_notSameInMemoryComponent() throws IOException {
    retrieveInitialAndUpdatedComponents(
        "InitialMountSpec.java",
        "UpdatedMountSpecWithDifferentInterface.java",
        (initialComponent, updatedComponent) ->
            assertThat(updatedComponent).isNotSameAs(initialComponent));
  }

  private void retrieveInitialAndUpdatedComponents(
      String fileName, String fileChangedName, BiConsumer<PsiClass, PsiClass> assertion)
      throws IOException {
    final PsiFile file = testHelper.configure(fileName);
    final PsiFile fileChanged = testHelper.configure(fileChangedName);
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass spec = LithoPluginUtils.getFirstClass(file, psiClass -> true).get();
              final PsiClass component = FileGenerateUtils.generateClass(spec);

              assertThat(component).isNotNull();

              final PsiClass updatedSpec =
                  LithoPluginUtils.getFirstClass(fileChanged, psiClass -> true).get();

              final PsiClass componentFromCachedModel =
                  FileGenerateUtils.generateClass(updatedSpec);
              assertThat(component).isEqualTo(componentFromCachedModel);

              // Update model before generating new class
              ComponentGenerateService.getInstance().getOrCreateSpecModel(updatedSpec, false);
              final PsiClass updatedComponent = FileGenerateUtils.generateClass(updatedSpec);
              assertion.accept(component, updatedComponent);
            });
  }
}
