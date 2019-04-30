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
package com.facebook.litho.intellij.completion;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase;
import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.codeInsight.generation.PsiGenerationInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.util.IncorrectOperationException;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Generates method. Doesn't prompt the user for additional data, uses pre-defined method instead.
 */
class MethodGenerateHandler extends GenerateMembersHandlerBase {
  private final PsiMethod generatedMethod;

  MethodGenerateHandler(PsiMethod method) {
    super("");
    generatedMethod = method;
  }

  @NotNull
  @Override
  protected List<? extends GenerationInfo> generateMemberPrototypes(
      PsiClass aClass, ClassMember[] members) throws IncorrectOperationException {
    return Collections.singletonList(new PsiGenerationInfo<PsiMember>(generatedMethod));
  }

  @Override
  protected ClassMember[] chooseOriginalMembers(PsiClass aClass, Project project, Editor editor) {
    return ClassMember.EMPTY_ARRAY;
  }

  @Override
  protected ClassMember[] getAllOriginalMembers(PsiClass aClass) {
    return ClassMember.EMPTY_ARRAY;
  }

  @Override
  protected GenerationInfo[] generateMemberPrototypes(PsiClass aClass, ClassMember originalMember)
      throws IncorrectOperationException {
    return GenerationInfo.EMPTY_ARRAY;
  }
}
