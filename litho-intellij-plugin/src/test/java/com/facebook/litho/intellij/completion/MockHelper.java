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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.openapi.vfs.VirtualFile;

class MockHelper {

  /**
   * Creates children-parent relationship for a sequence of {@link VirtualFile} mocks with given
   * names.
   *
   * @return a pair '[rootDirectory, endDirectory]' for the created sequence of {@link
   *     VirtualFile}s.
   */
  static VirtualFile[] createPathMocks(String... path) {
    VirtualFile rootDir = mock(VirtualFile.class);
    VirtualFile curDir = rootDir;
    for (String s : path) {
      VirtualFile nextDir = mock(VirtualFile.class);
      when(curDir.findChild(s)).thenReturn(nextDir);
      when(nextDir.getName()).thenReturn(s);
      when(nextDir.getParent()).thenReturn(curDir);
      curDir = nextDir;
    }
    VirtualFile[] dirs = new VirtualFile[2];
    dirs[0] = rootDir;
    dirs[1] = curDir;
    return dirs;
  }
}
