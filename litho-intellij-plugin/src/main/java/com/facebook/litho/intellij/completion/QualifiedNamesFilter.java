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

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import java.util.Collection;
import java.util.Optional;

public class QualifiedNamesFilter implements LookupElementFilter {
  private Collection<String> replacedQualifiedNames;
  private InsertHandler<LookupElement> insertHandler;

  public QualifiedNamesFilter(
      Collection<String> replacedQualifiedNames, InsertHandler<LookupElement> insertHandler) {
    this.replacedQualifiedNames = replacedQualifiedNames;
    this.insertHandler = insertHandler;
  }

  @Override
  public LookupElement apply(LookupElement lookupElement, PsiClass element) {
    return Optional.of(element)
        .map(PsiClass::getQualifiedName)
        .filter(replacedQualifiedNames::remove)
        .map(qualifiedName -> SpecLookupElement.create(lookupElement, qualifiedName, insertHandler))
        .orElse(null);
  }

  @Override
  public void addRemainingCompletions(Project project, CompletionResultSet result) {
    for (String qualifiedName : replacedQualifiedNames) {
      result.addElement(
          PrioritizedLookupElement.withPriority(
              SpecLookupElement.create(qualifiedName, project, insertHandler), Integer.MAX_VALUE));
    }
  }
}
