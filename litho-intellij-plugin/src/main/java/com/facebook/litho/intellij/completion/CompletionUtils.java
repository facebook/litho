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

import static com.intellij.patterns.StandardPatterns.or;

import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Optional;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

class CompletionUtils {
  static final ElementPattern<? extends PsiElement> METHOD_ANNOTATION_PARAMETER =
      PlatformPatterns.psiElement(PsiIdentifier.class)
          .inside(PsiAnnotationParameterList.class)
          .inside(PsiMethod.class);

  static final ElementPattern<? extends PsiElement> METHOD_PARAMETER_ANNOTATION =
      PlatformPatterns.psiElement(PsiIdentifier.class)
          .inside(PsiParameterList.class)
          .afterLeaf("@");

  static final ElementPattern<? extends PsiElement> METHOD_ANNOTATION =
      or(annotationInClass(), annotationAboveMethod());

  private static ElementPattern<? extends PsiElement> annotationAboveMethod() {
    // PsiIdentifier -> PsiJavaCodeReference -> PsiAnnotation -> PsiModifierList -> PsiMethod
    return PlatformPatterns.psiElement(PsiIdentifier.class)
        .withSuperParent(2, PsiAnnotation.class)
        .withSuperParent(4, PsiMethod.class)
        .afterLeaf("@");
  }

  private static ElementPattern<? extends PsiElement> annotationInClass() {
    // PsiIdentifier -> PsiJavaCodeReference -> PsiAnnotation -> PsiModifierList -> PsiClass
    return PlatformPatterns.psiElement(PsiIdentifier.class)
        .withSuperParent(2, PsiAnnotation.class)
        .withSuperParent(4, PsiClass.class)
        .afterLeaf("@");
  }

  static Optional<PsiClass> findFirstParent(PsiElement element, Predicate<PsiClass> condition) {
    return Optional.ofNullable(PsiTreeUtil.findFirstParent(element, PsiClass.class::isInstance))
        .map(PsiClass.class::cast)
        .filter(condition);
  }

  static final ElementPattern<? extends PsiElement> AFTER_DOT =
      PlatformPatterns.psiElement(PsiIdentifier.class).inside(PsiStatement.class).afterLeaf(".");

  /**
   * Creates new {@link CompletionResult}.
   *
   * @param completionResult provides matcher, and sorter for new instance
   * @param lookup is used in new instance instead of {@link LookupElement} from the provided
   *     completionResult
   * @return new {@link CompletionResult} or null, if matcher doesn't match new lookup
   */
  @Nullable
  static CompletionResult wrap(CompletionResult completionResult, LookupElement lookup) {
    return CompletionResult.wrap(
        lookup, completionResult.getPrefixMatcher(), completionResult.getSorter());
  }
}
