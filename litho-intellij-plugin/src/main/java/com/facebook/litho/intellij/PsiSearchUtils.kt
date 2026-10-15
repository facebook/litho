/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.annotations.TestOnly

abstract class PsiSearchUtils {

  /**
   * Searches everywhere for a java class with the specified full-qualified name and returns one if
   * it is found (excluding .class). This method might return classes out of project scope.
   *
   * @see .findClass
   */
  abstract fun findOriginalClass(project: Project, qualifiedName: String): PsiClass?

  /**
   * Searches everywhere for a class with the specified full-qualified name and returns one if it is
   * found (including .class). This method might return classes out of project scope.
   *
   * @param qualifiedName the full-qualified name of the class to find.
   * @return the PSI class, or null if no class with such name is found.
   */
  abstract fun findClass(project: Project, qualifiedName: String): PsiClass?

  /**
   * Searches everywhere for classes with the specified full-qualified name and returns all found
   * classes (including .class).
   *
   * @param qualifiedName the full-qualified name of the class to find.
   * @return the array of found classes, or an empty array if no classes are found.
   */
  abstract fun findClasses(project: Project, qualifiedName: String): Array<PsiClass>

  /**
   * Returns the list of all classes with the specified name in the specified scope
   *
   * @param project the project that includes scope.
   * @param searchScope the scope in which classes are searched.
   * @param name the non-qualified name of the classes to find.
   * @return the list of found classes.
   */
  abstract fun findClassesByShortName(
      project: Project,
      searchScope: GlobalSearchScope,
      name: String,
  ): Array<PsiClass>

  /** For testing */
  @TestOnly open fun addMock(name: String, cls: PsiClass): Unit = Unit

  /** For testing */
  @TestOnly open fun clearMocks(): Unit = Unit

  private class PsiSearchUtilsImpl : PsiSearchUtils() {

    override fun findOriginalClass(project: Project, qualifiedName: String): PsiClass? {
      return findClasses(project, qualifiedName).firstOrNull { it !is ClsClassImpl }
    }

    override fun findClass(project: Project, qualifiedName: String): PsiClass? {
      return JavaPsiFacade.getInstance(project)
          .findClass(qualifiedName, GlobalSearchScope.everythingScope(project))
    }

    override fun findClasses(project: Project, qualifiedName: String): Array<PsiClass> {
      return JavaPsiFacade.getInstance(project)
          .findClasses(qualifiedName, GlobalSearchScope.everythingScope(project))
    }

    override fun findClassesByShortName(
        project: Project,
        searchScope: GlobalSearchScope,
        name: String,
    ): Array<PsiClass> {
      return PsiShortNamesCache.getInstance(project).getClassesByName(name, searchScope)
    }

    companion object {
      val INSTANCE: PsiSearchUtils = PsiSearchUtilsImpl()
    }
  }

  private class MockPsiSearchUtils : PsiSearchUtils() {
    @TestOnly private val mockMap: MutableMap<String, PsiClass> = HashMap()

    override fun findOriginalClass(project: Project, qualifiedName: String): PsiClass? {
      return findClass(project, qualifiedName)
    }

    override fun findClass(project: Project, qualifiedName: String): PsiClass? {
      return mockMap[qualifiedName]
    }

    override fun findClasses(project: Project, qualifiedName: String): Array<PsiClass> {
      val found = findClass(project, qualifiedName) ?: return PsiClass.EMPTY_ARRAY
      return arrayOf(found)
    }

    override fun findClassesByShortName(
        project: Project,
        searchScope: GlobalSearchScope,
        name: String,
    ): Array<PsiClass> {
      return findClasses(project, name)
    }

    override fun addMock(name: String, cls: PsiClass) {
      mockMap[name] = cls
    }

    override fun clearMocks() {
      mockMap.clear()
    }

    companion object {
      val INSTANCE: PsiSearchUtils = MockPsiSearchUtils()
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(): PsiSearchUtils {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        return MockPsiSearchUtils.INSTANCE
      } else {
        return PsiSearchUtilsImpl.INSTANCE
      }
    }
  }
}
