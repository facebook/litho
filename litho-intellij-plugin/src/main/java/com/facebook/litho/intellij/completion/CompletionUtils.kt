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

package com.facebook.litho.intellij.completion

import com.intellij.codeInsight.completion.CompletionResult
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiStatement
import com.intellij.psi.util.PsiTreeUtil
import java.util.Optional
import java.util.function.Predicate
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

internal object CompletionUtils {
  @JvmField
  val METHOD_ANNOTATION_PARAMETER: ElementPattern<out PsiElement> =
      PlatformPatterns.psiElement(PsiIdentifier::class.java)
          .inside(PsiAnnotationParameterList::class.java)
          .inside(PsiMethod::class.java)

  @JvmField
  val IN_KT_EXPR: ElementPattern<out PsiElement> =
      PlatformPatterns.psiElement(PsiElement::class.java)
          .inside(KtNameReferenceExpression::class.java)

  @JvmField
  val METHOD_PARAMETER_ANNOTATION: ElementPattern<out PsiElement> =
      PlatformPatterns.psiElement(PsiIdentifier::class.java)
          .inside(PsiParameterList::class.java)
          .afterLeaf("@")

  @JvmField
  val METHOD_ANNOTATION: ElementPattern<out PsiElement> =
      or(annotationInClass(), annotationAboveMethod())

  @JvmField
  val AFTER_DOT: ElementPattern<out PsiElement> =
      PlatformPatterns.psiElement(PsiIdentifier::class.java)
          .inside(PsiStatement::class.java)
          .afterLeaf(".")

  private fun annotationAboveMethod(): ElementPattern<out PsiElement> {
    // PsiIdentifier -> PsiJavaCodeReference -> PsiAnnotation -> PsiModifierList -> PsiMethod
    return PlatformPatterns.psiElement(PsiIdentifier::class.java)
        .withSuperParent(2, PsiAnnotation::class.java)
        .withSuperParent(4, PsiMethod::class.java)
        .afterLeaf("@")
  }

  private fun annotationInClass(): ElementPattern<out PsiElement> {
    // PsiIdentifier -> PsiJavaCodeReference -> PsiAnnotation -> PsiModifierList -> PsiClass
    return PlatformPatterns.psiElement(PsiIdentifier::class.java)
        .withSuperParent(2, PsiAnnotation::class.java)
        .withSuperParent(4, PsiClass::class.java)
        .afterLeaf("@")
  }

  @JvmStatic
  fun findFirstParent(element: PsiElement, condition: Predicate<PsiClass>): Optional<PsiClass> {
    return Optional.ofNullable(
            PsiTreeUtil.findFirstParent(element, PsiClass::class.java::isInstance),
        )
        .map(PsiClass::class.java::cast)
        .filter(condition)
  }

  /**
   * Creates new [CompletionResult].
   *
   * @param completionResult provides matcher, and sorter for new instance
   * @param lookup is used in new instance instead of [LookupElement] from the provided
   *   completionResult
   * @return new [CompletionResult] or null, if matcher doesn't match new lookup
   */
  @JvmStatic
  fun wrap(completionResult: CompletionResult, lookup: LookupElement): CompletionResult? {
    return CompletionResult.wrap(lookup, completionResult.prefixMatcher, completionResult.sorter)
  }
}
