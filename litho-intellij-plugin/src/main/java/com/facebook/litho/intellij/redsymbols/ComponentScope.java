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

import static com.facebook.litho.intellij.LithoPluginUtils.getVirtualFile;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * Contains generated Litho files only. It is meant to enlarge other scope.
 *
 * @see ComponentResolveScopeEnlarger
 */
public class ComponentScope extends GlobalSearchScope {
  private static final Key<Boolean> KEY = Key.create("com.facebook.litho.intellij.file.Component");
  private static final Logger LOG = Logger.getInstance(ComponentScope.class);

  public static ComponentScope getInstance() {
    return Holder.INSTANCE;
  }

  /** Creates new dummy file included in the scope. */
  static VirtualFile createDummyFile(Project project) {
    final PsiFile dummyFile =
        PsiFileFactory.getInstance(project)
            .createFileFromText("Dummy.java", StdFileTypes.JAVA, "class Dummy {}");
    return include(dummyFile);
  }

  /**
   * Includes {@link VirtualFile} of the given {@link PsiFile} in the scope and returns included
   * file.
   */
  public static VirtualFile include(PsiFile file) {
    final VirtualFile vf = getVirtualFile(file);
    vf.putUserData(KEY, true);
    return vf;
  }

  public static boolean contains(PsiFile file) {
    return containsInternal(getVirtualFile(file));
  }

  private static boolean containsInternal(VirtualFile file) {
    return StdFileTypes.JAVA.equals(file.getFileType()) && file.getUserData(KEY) != null;
  }

  @Override
  public int compare(VirtualFile file1, VirtualFile file2) {
    boolean isInScope1 = contains(file1);
    boolean isInScope2 = contains(file2);
    if (isInScope1 == isInScope2) return 0;
    if (isInScope1) return -1;
    return 1;
  }

  @Override
  public boolean isSearchInModuleContent(Module aModule) {
    return false;
  }

  @Override
  public boolean isSearchInLibraries() {
    return false;
  }

  @Override
  public boolean contains(VirtualFile file) {
    final boolean contains = containsInternal(file);
    LOG.debug("contains " + file.getName() + " " + contains);
    return contains;
  }

  static class Holder {
    static ComponentScope INSTANCE = new ComponentScope();
  }
}
