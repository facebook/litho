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

package com.facebook.litho.intellij.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.SearchScope;
import java.io.File;
import java.util.function.Function;
import org.junit.Test;

public class LithoFindUsagesHandlerFactoryTest extends LithoPluginIntellijTest {

  public LithoFindUsagesHandlerFactoryTest() {
    super("testdata/file");
  }

  @Test
  public void canFindUsages() {
    testHelper.getPsiClass(
        psiClasses -> {
          assertNotNull(psiClasses);
          PsiClass layoutSpec = psiClasses.get(0);
          PsiClass otherSpec = psiClasses.get(1);
          PsiClass notSpec = psiClasses.get(2);

          LithoFindUsagesHandlerFactory factory = new LithoFindUsagesHandlerFactory();
          assertTrue(factory.canFindUsages(layoutSpec));
          assertTrue(factory.canFindUsages(otherSpec));
          assertFalse(factory.canFindUsages(notSpec));

          return true;
        },
        "LayoutSpec.java",
        "MountSpec.java",
        "NotSpec.java");
  }

  @Test
  public void findUsages() {
    PsiClass mockedElement = mock(PsiClass.class);
    PsiClass mockedResult = mock(PsiClass.class);

    // We mock this function because project search is not working in test environment
    Function<PsiClass, PsiClass> findGeneratedComponent = psiClass -> mockedResult;
    LithoFindUsagesHandlerFactory.GeneratedClassFindUsagesHandler handler =
        new LithoFindUsagesHandlerFactory.GeneratedClassFindUsagesHandler(
            mockedElement, findGeneratedComponent);

    // Includes both original element and "generated component" element
    PsiElement[] primaryElements = handler.getPrimaryElements();
    assertEquals(2, primaryElements.length);
    assertEquals(mockedResult, primaryElements[0]);
    assertEquals(mockedElement, primaryElements[1]);
  }

  @Test
  public void getFindUsagesOptions() {
    testHelper.getPsiClass(
        psiClasses -> {
          assertNotNull(psiClasses);
          PsiClass layoutSpec = psiClasses.get(0);

          // Associate generatedComponentVirtualFile with mockedGeneratedComponentCls
          PsiClass mockedGeneratedComponentCls = mock(PsiClass.class);
          PsiFile mockedGeneratedComponentFile = mock(PsiFile.class);
          when(mockedGeneratedComponentCls.getContainingFile())
              .thenReturn(mockedGeneratedComponentFile);
          VirtualFile generatedComponentVirtualFile = createPresentInScopeVirtualFile();
          when(mockedGeneratedComponentFile.getVirtualFile())
              .thenReturn(generatedComponentVirtualFile);

          VirtualFile presentInScopeVirtualFile = createPresentInScopeVirtualFile();

          // We mock this function because project search is not working in test environment
          Function<PsiClass, PsiClass> findGeneratedComponent =
              psiClass -> mockedGeneratedComponentCls;
          LithoFindUsagesHandlerFactory.GeneratedClassFindUsagesHandler handler =
              new LithoFindUsagesHandlerFactory.GeneratedClassFindUsagesHandler(
                  layoutSpec, findGeneratedComponent);

          // Search scope should not contain generated component
          SearchScope searchScope = handler.getFindUsagesOptions(null).searchScope;
          assertTrue(searchScope.contains(presentInScopeVirtualFile));
          assertFalse(searchScope.contains(generatedComponentVirtualFile));

          return true;
        },
        "LayoutSpec.java");
  }

  private VirtualFile createPresentInScopeVirtualFile() {
    return new PresentInScopeVirtualFile(mock(CoreLocalFileSystem.class), mock(File.class));
  }

  /**
   * {@link com.intellij.psi.search.ProjectScopeImpl#contains(VirtualFile)} implementation returns
   * true if given file instanceof VirtualFileWindow.
   */
  class PresentInScopeVirtualFile extends CoreLocalVirtualFile implements VirtualFileWindow {

    PresentInScopeVirtualFile(CoreLocalFileSystem fileSystem, File ioFile) {
      super(fileSystem, ioFile);
    }

    @Override
    public VirtualFile getDelegate() {
      return null;
    }

    @Override
    public DocumentWindow getDocumentWindow() {
      return null;
    }
  }
}
