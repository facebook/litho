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

import static com.intellij.patterns.StandardPatterns.or;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Optional;
import java.util.function.Predicate;

class CompletionUtils {
  static final ElementPattern<PsiElement> METHOD_ANNOTATION =
      or(annotationInClass(), annotationAboveMethod());

  private static ElementPattern<? extends PsiElement> annotationAboveMethod() {
    // PsiIdentifier -> PsiJavaCodeReference -> PsiAnnotation -> PsiModifierList -> PsiMethod
    return PlatformPatterns.psiElement(PsiIdentifier.class)
        .withSuperParent(2, PsiAnnotation.class)
        .withSuperParent(4, PsiMethod.class)
        .afterLeaf("@")
        .withLanguage(JavaLanguage.INSTANCE);
  }

  private static ElementPattern<? extends PsiElement> annotationInClass() {
    // PsiIdentifier -> PsiJavaCodeReference -> PsiAnnotation -> PsiModifierList -> PsiClass
    return PlatformPatterns.psiElement(PsiIdentifier.class)
        .withSuperParent(2, PsiAnnotation.class)
        .withSuperParent(4, PsiClass.class)
        .afterLeaf("@")
        .withLanguage(JavaLanguage.INSTANCE);
  }

  static Optional<PsiClass> findFirstParent(PsiElement element, Predicate<PsiClass> condition) {
    return Optional.ofNullable(PsiTreeUtil.findFirstParent(element, PsiClass.class::isInstance))
        .map(PsiClass.class::cast)
        .filter(condition);
  }
}
