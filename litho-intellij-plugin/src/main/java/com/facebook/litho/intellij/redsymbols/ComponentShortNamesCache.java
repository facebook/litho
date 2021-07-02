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
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import java.util.Arrays;

class ComponentShortNamesCache extends PsiShortNamesCache {
  private final Project project;
  private final VirtualFile dummyFile;

  ComponentShortNamesCache(Project project) {
    this.project = project;
    this.dummyFile = ComponentScope.createDummyFile(project);
  }

  @Override
  public PsiClass[] getClassesByName(String name, GlobalSearchScope scope) {
    if (!scope.contains(dummyFile)) return PsiClass.EMPTY_ARRAY;

    return Arrays.stream(ComponentsCacheService.getInstance(project).getAllComponents())
        .filter(
            cls -> {
              final String shortName = StringUtil.getShortName(cls.getQualifiedName());
              return shortName.equals(name);
            })
        .toArray(PsiClass[]::new);
  }

  @Override
  public String[] getAllClassNames() {
    return Arrays.stream(ComponentsCacheService.getInstance(project).getAllComponents())
        .map(cls -> StringUtil.getShortName(cls.getQualifiedName()))
        .toArray(String[]::new);
  }

  @Override
  public PsiMethod[] getMethodsByName(String name, GlobalSearchScope scope) {
    return PsiMethod.EMPTY_ARRAY;
  }

  @Override
  public PsiMethod[] getMethodsByNameIfNotMoreThan(
      String name, GlobalSearchScope scope, int maxCount) {
    return PsiMethod.EMPTY_ARRAY;
  }

  @Override
  public PsiField[] getFieldsByNameIfNotMoreThan(
      String name, GlobalSearchScope scope, int maxCount) {
    return PsiField.EMPTY_ARRAY;
  }

  @Override
  public boolean processMethodsWithName(
      String name, GlobalSearchScope scope, Processor<? super PsiMethod> processor) {
    return false;
  }

  @Override
  public String[] getAllMethodNames() {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Override
  public PsiField[] getFieldsByName(String name, GlobalSearchScope scope) {
    return PsiField.EMPTY_ARRAY;
  }

  @Override
  public String[] getAllFieldNames() {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }
}
