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

package com.facebook.litho.intellij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

public abstract class PsiSearchUtils {

  public static PsiSearchUtils getInstance() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return MockPsiSearchUtils.INSTANCE;
    } else {
      return PsiSearchUtilsImpl.INSTANCE;
    }
  }

  /**
   * Searches everywhere for a java class with the specified full-qualified name and returns one if
   * it is found (excluding .class). This method might return classes out of project scope.
   *
   * @see #findClass(Project, String)
   */
  @Nullable
  public abstract PsiClass findOriginalClass(Project project, String qualifiedName);

  /**
   * Searches everywhere for a class with the specified full-qualified name and returns one if it is
   * found (including .class). This method might return classes out of project scope.
   *
   * @param qualifiedName the full-qualified name of the class to find.
   * @return the PSI class, or null if no class with such name is found.
   */
  @Nullable
  public abstract PsiClass findClass(Project project, String qualifiedName);

  /**
   * Searches everywhere for classes with the specified full-qualified name and returns all found
   * classes (including .class).
   *
   * @param qualifiedName the full-qualified name of the class to find.
   * @return the array of found classes, or an empty array if no classes are found.
   */
  public abstract PsiClass[] findClasses(Project project, String qualifiedName);

  /**
   * Returns the list of all classes with the specified name in the specified scope
   *
   * @param project the project that includes scope.
   * @param searchScope the scope in which classes are searched.
   * @param name the non-qualified name of the classes to find.
   * @return the list of found classes.
   */
  public abstract PsiClass[] findClassesByShortName(
      Project project, GlobalSearchScope searchScope, String name);

  /** For testing */
  @TestOnly
  public void addMock(String name, PsiClass cls) {}

  /** For testing */
  @TestOnly
  public void clearMocks() {}

  private static class PsiSearchUtilsImpl extends PsiSearchUtils {
    private static final PsiSearchUtils INSTANCE = new PsiSearchUtilsImpl();

    @Override
    public @Nullable PsiClass findOriginalClass(Project project, String qualifiedName) {
      return Arrays.stream(findClasses(project, qualifiedName))
          .filter(cls -> !(cls instanceof ClsClassImpl))
          .findAny()
          .orElse(null);
    }

    @Override
    public @Nullable PsiClass findClass(Project project, String qualifiedName) {
      return JavaPsiFacade.getInstance(project)
          .findClass(qualifiedName, GlobalSearchScope.everythingScope(project));
    }

    @Override
    public PsiClass[] findClasses(Project project, String qualifiedName) {
      return JavaPsiFacade.getInstance(project)
          .findClasses(qualifiedName, GlobalSearchScope.everythingScope(project));
    }

    @Override
    public PsiClass[] findClassesByShortName(
        Project project, GlobalSearchScope searchScope, String name) {
      return PsiShortNamesCache.getInstance(project).getClassesByName(name, searchScope);
    }
  }

  private static class MockPsiSearchUtils extends PsiSearchUtils {
    private static final PsiSearchUtils INSTANCE = new MockPsiSearchUtils();
    @TestOnly private final Map<String, PsiClass> mockMap = new HashMap<>();

    @Override
    public @Nullable PsiClass findOriginalClass(Project project, String qualifiedName) {
      return findClass(project, qualifiedName);
    }

    @Override
    public @Nullable PsiClass findClass(Project project, String qualifiedName) {
      return mockMap.get(qualifiedName);
    }

    @Override
    public PsiClass[] findClasses(Project project, String qualifiedName) {
      final PsiClass found = findClass(project, qualifiedName);
      if (found == null) {
        return PsiClass.EMPTY_ARRAY;
      }
      return new PsiClass[] {found};
    }

    @Override
    public PsiClass[] findClassesByShortName(
        Project project, GlobalSearchScope searchScope, String name) {
      return findClasses(project, name);
    }

    @Override
    public void addMock(String name, PsiClass cls) {
      mockMap.put(name, cls);
    }

    @Override
    public void clearMocks() {
      mockMap.clear();
    }
  }
}
