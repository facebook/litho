/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import javax.annotation.Nullable;

public class PsiSearchUtils {

  /**
   * Searches global project scope for a class with the specified full-qualified name and returns
   * one if it is found.
   *
   * @param project to find the class in.
   * @param qualifiedName the full-qualified name of the class to find.
   * @return the PSI class, or null if no class with such name is found.
   */
  @Nullable
  public static PsiClass findClass(Project project, String qualifiedName) {
    return JavaPsiFacade.getInstance(project)
        .findClass(qualifiedName, GlobalSearchScope.everythingScope(project));
  }
}
