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

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.mock.MockPsiDirectory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ComponentGenerateUtilsTest extends LithoPluginIntellijTest {

  public ComponentGenerateUtilsTest() {
    super("testdata");
  }

  @Test
  public void getDirectoryPath() {
    Project project = Mockito.mock(Project.class);
    VirtualFile[] dirs = createPathMocks("app", "com", "example");
    Mockito.when(project.getBaseDir()).thenReturn(dirs[0]);
    MockPsiDirectory mockedDirectory =
        new MockPsiDirectory(project, Mockito.mock(Disposable.class)) {
          @Override
          public VirtualFile getVirtualFile() {
            return dirs[1];
          }
        };
    Assert.assertEquals(
        "app/com/example", ComponentGenerateUtils.getDirectoryPath(mockedDirectory));
  }
}
