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

import com.intellij.lang.ASTNode
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.AnnotationSession
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

internal class TestHolder : AnnotationHolder {

  @JvmField val errorElements: MutableList<PsiElement> = ArrayList()
  @JvmField val errorMessages: MutableList<String?> = ArrayList()
  @JvmField val createdAnnotations: MutableList<Annotation> = ArrayList()

  @Suppress("DEPRECATION", "DeprecatedMethod")
  override fun createErrorAnnotation(elt: PsiElement, message: String?): Annotation {
    errorElements.add(elt)
    errorMessages.add(message)
    val stub = Annotation(0, 0, HighlightSeverity.ERROR, "", "")
    createdAnnotations.add(stub)
    return stub
  }

  override fun createErrorAnnotation(node: ASTNode, message: String?): Annotation? {
    return null
  }

  override fun createErrorAnnotation(range: TextRange, message: String?): Annotation? {
    return null
  }

  override fun createWarningAnnotation(elt: PsiElement, message: String?): Annotation? {
    return null
  }

  override fun createWarningAnnotation(node: ASTNode, message: String?): Annotation? {
    return null
  }

  override fun createWarningAnnotation(range: TextRange, message: String?): Annotation? {
    return null
  }

  override fun createWeakWarningAnnotation(elt: PsiElement, message: String?): Annotation? {
    return null
  }

  // This method is removed in the newer APIs, but is present in the older ones, so let's keep it
  // for now and remove it later.
  // TODO: remove this method
  fun createWeakWarningAnnotation(node: ASTNode, message: String?): Annotation? {
    return null
  }

  override fun createWeakWarningAnnotation(range: TextRange, message: String?): Annotation? {
    return null
  }

  override fun createInfoAnnotation(elt: PsiElement, message: String?): Annotation? {
    return null
  }

  override fun createInfoAnnotation(node: ASTNode, message: String?): Annotation? {
    return null
  }

  override fun createInfoAnnotation(range: TextRange, message: String?): Annotation? {
    return null
  }

  override fun createAnnotation(
      severity: HighlightSeverity,
      range: TextRange,
      message: String?,
  ): Annotation? {
    return null
  }

  override fun createAnnotation(
      severity: HighlightSeverity,
      range: TextRange,
      message: String?,
      htmlTooltip: String?,
  ): Annotation? {
    return null
  }

  override fun getCurrentAnnotationSession(): AnnotationSession {
    throw UnsupportedOperationException()
  }

  override fun isBatchMode(): Boolean {
    return false
  }
}
