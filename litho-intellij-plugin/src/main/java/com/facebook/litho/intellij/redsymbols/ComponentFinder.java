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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.Arrays;
import org.jetbrains.annotations.Nullable;

/** Locates Litho component by full-qualified name, or null if it wasn't found. */
public class ComponentFinder extends PsiElementFinder {
  private final Project project;
  private final VirtualFile dummyFile;

  ComponentFinder(Project project) {
    this.project = project;
    this.dummyFile = ComponentScope.createDummyFile(project);
  }

  @Override
  public PsiClass[] findClasses(String qualifiedName, GlobalSearchScope scope) {
    final PsiClass component = findClassInternal(qualifiedName, scope, project);
    if (component == null) {
      return PsiClass.EMPTY_ARRAY;
    }
    return new PsiClass[] {component};
  }

  @Nullable
  @Override
  public PsiClass findClass(String qualifiedName, GlobalSearchScope scope) {
    return findClassInternal(qualifiedName, scope, project);
  }

  @Override
  public PsiClass[] getClasses(PsiPackage psiPackage, GlobalSearchScope scope) {
    if (!scope.contains(dummyFile)) return PsiClass.EMPTY_ARRAY;

    // We don't create own package, but provide additional classes to existing one
    final String packageQN = psiPackage.getQualifiedName();
    return Arrays.stream(ComponentsCacheService.getInstance(project).getAllComponents())
        .filter(cls -> StringUtil.getPackageName(cls.getQualifiedName()).equals(packageQN))
        .toArray(PsiClass[]::new);
  }

  @Nullable
  private PsiClass findClassInternal(
      String qualifiedName, GlobalSearchScope scope, Project project) {
    if (!scope.contains(dummyFile)) return null;

    if (!StringUtil.isJavaIdentifier(StringUtil.getShortName(qualifiedName))) return null;

    final PsiClass componentFromCache =
        ComponentsCacheService.getInstance(project).getComponent(qualifiedName);

    return componentFromCache;
  }
}
