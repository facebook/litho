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

package com.facebook.litho.intellij.inspections

import com.facebook.litho.specmodels.model.SpecModelValidationError
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner

object AnnotatorUtils {

  /** Adds error annotation to the provided holder. */
  @JvmStatic
  fun addError(holder: AnnotationHolder, error: SpecModelValidationError) {
    addError(holder, error, emptyList())
  }

  @JvmStatic
  fun addError(
      holder: AnnotationHolder,
      error: SpecModelValidationError,
      fixes: List<IntentionAction>,
  ) {
    val errorElement = error.element as PsiElement
    val annotationTarget =
        if (errorElement is PsiClass || errorElement is PsiMethod) {
          (errorElement as PsiNameIdentifierOwner).nameIdentifier ?: errorElement
        } else {
          errorElement
        }
    val errorAnnotation = holder.createErrorAnnotation(annotationTarget, error.message)
    if (fixes.isNotEmpty()) {
      for (fix in fixes) {
        errorAnnotation.registerFix(fix)
      }
    }
  }
}
