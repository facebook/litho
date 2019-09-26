/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.intellij.file;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ComponentFileListenerTest extends LithoPluginIntellijTest {

  public ComponentFileListenerTest() {
    super("testdata/file");
  }

  @Test
  public void beforeLayoutSpecSaving() {
    AtomicBoolean invoked = new AtomicBoolean(false);
    ComponentFileListener componentFileListener =
        new ComponentFileListener(
            psiClass -> {
              invoked.set(true);
            });

    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass psiCls = psiClasses.get(0);

          PsiFile containingFile = psiCls.getContainingFile();
          componentFileListener.beforeFileSaving(containingFile);
          return true;
        },
        "LayoutSpec.java");

    Assert.assertTrue(invoked.get());
  }

  @Test
  public void beforeNotLayoutSpecSaving() {
    AtomicBoolean invoked = new AtomicBoolean(false);
    ComponentFileListener componentFileListener =
        new ComponentFileListener(
            psiClass -> {
              invoked.set(true);
            });

    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass psiCls = psiClasses.get(0);

          PsiFile containingFile = psiCls.getContainingFile();
          componentFileListener.beforeFileSaving(containingFile);
          return true;
        },
        "MountSpec.java");

    Assert.assertFalse(invoked.get());
  }

  @Test
  public void isComponent() {
    ComponentFileListener componentFileListener = new ComponentFileListener();

    // We can't get PsiFile with super parent for testing, thant's why mocking whole hierarchy
    PsiClass mockedComponent = Mockito.mock(PsiClass.class);
    Mockito.when(mockedComponent.getName()).thenReturn("ComponentLifecycle");

    PsiClass mockedSubComponent = Mockito.mock(PsiClass.class);
    Mockito.when(mockedSubComponent.getSuperClass()).thenReturn(mockedComponent);

    PsiFile mockedSubComponentFile = Mockito.mock(PsiFile.class);
    Mockito.when(mockedSubComponentFile.getFirstChild()).thenReturn(mockedSubComponent);

    // TODO: T48863460 revisit update flow
    //    Assert.assertFalse(componentFileListener.notComponent(mockedSubComponentFile));
  }

  @Test
  public void isNotComponent() {
    ComponentFileListener componentFileListener = new ComponentFileListener();

    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass psiCls = psiClasses.get(0);

          PsiFile containingFile = psiCls.getContainingFile();

          // TODO: T48863460 revisit update flow
          //          Assert.assertTrue(componentFileListener.notComponent(containingFile));
          return true;
        },
        "LayoutSpec.java");
  }
}
