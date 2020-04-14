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

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import java.util.HashMap;
import java.util.Map;

public class PsiSearchUtils {
  static Map<String, PsiClass> mockMap = new HashMap<>();

  public static void addMock(String name, PsiClass cls) {
    mockMap.put(name, cls);
  }

  public static PsiClass findClass(Project project, String name) {
    return mockMap.get(name);
  }

  public static PsiClass[] findClasses(Project project, String qualifiedName) {
    return new PsiClass[] {findClass(project, qualifiedName)};
  }
}
