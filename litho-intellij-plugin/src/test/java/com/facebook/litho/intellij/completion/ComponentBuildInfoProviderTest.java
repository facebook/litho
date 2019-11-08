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

package com.facebook.litho.intellij.completion;

import static com.facebook.litho.intellij.completion.MockHelper.createPathMocks;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.extensions.BuildInfoProvider;
import com.intellij.mock.MockPsiManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import java.util.Optional;
import org.junit.Test;

public class ComponentBuildInfoProviderTest extends LithoPluginIntellijTest {

  public ComponentBuildInfoProviderTest() {
    super("testdata");
  }

  @Test
  public void isValidDirForPackage_valid() {
    assertTrue(ComponentBuildInfoProvider.isValidDirForPackage("app/com/example", "com.example"));
    assertTrue(ComponentBuildInfoProvider.isValidDirForPackage("example", "example"));
  }

  @Test
  public void isValidDirForPackage_invalidDirPath() {
    assertFalse(ComponentBuildInfoProvider.isValidDirForPackage(null, null));
    assertFalse(ComponentBuildInfoProvider.isValidDirForPackage("app.com.example", "com.example"));
  }

  @Test
  public void findTargetDirectory_exists() {
    VirtualFile[] mocks = createPathMocks("com", "example");

    assertEquals(mocks[1], ComponentBuildInfoProvider.findTargetDirectory("com/example", mocks[0]));
  }

  @Test
  public void findTargetDirectory_doesNotExist() {
    VirtualFile rootDir = mock(VirtualFile.class);
    assertNull(ComponentBuildInfoProvider.findTargetDirectory("com/example", rootDir));

    VirtualFile[] mocks = createPathMocks("com", "test1", "test2");
    assertNull(ComponentBuildInfoProvider.findTargetDirectory("com/example", mocks[0]));
  }

  @Test
  public void findTargetDirectory_invalidPath() {
    VirtualFile[] mocks = createPathMocks("com", "example", "test");
    assertNull(ComponentBuildInfoProvider.findTargetDirectory("com.example.test", mocks[0]));
  }

  @Test
  public void provideGeneratedComponentDirs_valid() {
    BuildInfoProvider[] providers = {(dir, packageName) -> "app/com/test/example"};
    VirtualFile[] mocks = createPathMocks("app", "com", "test", "example");
    Project mockProject = mock(Project.class);
    MockPsiManager mockPsiManager = new MockPsiManager(mockProject);
    PsiDirectory mockDirectory = mock(PsiDirectory.class);
    mockPsiManager.addPsiDirectory(mocks[1], mockDirectory);

    PsiDirectory found =
        ComponentBuildInfoProvider.provideGeneratedComponentDirs(
                providers, "com/test/example", "com.test.example", mocks[0], mockPsiManager)
            .findFirst()
            .get();

    assertEquals(mockDirectory, found);
  }

  @Test
  public void provideGeneratedComponentDirs_doesNotExist() {
    BuildInfoProvider[] providers = {(dir, packageName) -> "app/com/my/example"};
    VirtualFile[] mocks = createPathMocks("app", "com", "test", "example");
    Project mockProject = mock(Project.class);
    MockPsiManager mockPsiManager = new MockPsiManager(mockProject);
    PsiDirectory mockDirectory = mock(PsiDirectory.class);
    mockPsiManager.addPsiDirectory(mocks[1], mockDirectory);

    Optional<PsiDirectory> found =
        ComponentBuildInfoProvider.provideGeneratedComponentDirs(
                providers, "com/test/example", "com.test.example", mocks[0], mockPsiManager)
            .findFirst();

    assertFalse(found.isPresent());
  }

  @Test
  public void getContainingDirectory_valid() {
    PsiClass mockCls = mock(PsiClass.class);
    PsiJavaFile mockFile = mock(PsiJavaFile.class);
    PsiDirectory mockDir = mock(PsiDirectory.class);
    when(mockCls.getContainingFile()).thenReturn(mockFile);
    when(mockFile.getContainingDirectory()).thenReturn(mockDir);
    assertEquals(
        mockDir, ComponentBuildInfoProvider.getContainingDirectory(mockCls).findAny().get());
  }

  @Test
  public void getContainingDirectory_invalidFile() {
    PsiClass mockCls = mock(PsiClass.class);
    PsiFile mockFile = mock(PsiFile.class);
    PsiDirectory mockDir = mock(PsiDirectory.class);
    when(mockCls.getContainingFile()).thenReturn(mockFile);
    when(mockFile.getContainingDirectory()).thenReturn(mockDir);
    assertEquals(0, ComponentBuildInfoProvider.getContainingDirectory(mockCls).count());
  }
}
