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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ResolveScopeEnlarger;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.Nullable;

/**
 * Enlarges the scope of files under a content root with generated Litho files.
 *
 * @see ComponentScope
 */
class ComponentResolveScopeEnlarger extends ResolveScopeEnlarger {

  @Nullable
  @Override
  public SearchScope getAdditionalResolveScope(VirtualFile file, Project project) {
    if (ProjectRootManager.getInstance(project).getFileIndex().isInContent(file)) {
      return ComponentScope.getInstance();
    }
    return null;
  }
}
