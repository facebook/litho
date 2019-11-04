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

package com.facebook.litho.intellij.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import org.junit.Test;
import org.mockito.Mockito;

public class GenerateComponentActionTest extends LithoPluginIntellijTest {

  public GenerateComponentActionTest() {
    super("testdata/file");
  }

  @Test
  public void getValidSpec_layoutspec() {
    testHelper.getPsiClass(
        psiClasses -> {
          AnActionEvent mock = createEvent(psiClasses.get(0));
          assertTrue(GenerateComponentAction.getValidSpec(mock).isPresent());
          return true;
        },
        "LayoutSpec.java");
  }

  @Test
  public void getValidSpec_mountspec() {
    testHelper.getPsiClass(
        psiClasses -> {
          AnActionEvent mock = createEvent(psiClasses.get(0));
          assertFalse(GenerateComponentAction.getValidSpec(mock).isPresent());
          return true;
        },
        "MountSpec.java");
  }

  @Test
  public void getValidSpec_notspec() {
    testHelper.getPsiClass(
        psiClasses -> {
          AnActionEvent mock = createEvent(psiClasses.get(0));
          assertFalse(GenerateComponentAction.getValidSpec(mock).isPresent());
          return true;
        },
        "NotSpec.java");
  }

  private static AnActionEvent createEvent(PsiClass content) {
    PsiFile containingFile = content.getContainingFile();
    AnActionEvent mock = mock(AnActionEvent.class);
    Mockito.when(mock.getData(CommonDataKeys.PSI_FILE)).thenReturn(containingFile);
    return mock;
  }
}
