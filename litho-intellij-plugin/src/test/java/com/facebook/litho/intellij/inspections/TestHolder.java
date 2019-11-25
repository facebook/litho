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

package com.facebook.litho.intellij.inspections;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

class TestHolder implements AnnotationHolder {
  final List<PsiElement> errorElements = new ArrayList<>();
  final List<String> errorMessages = new ArrayList<>();
  final List<Annotation> createdAnnotations = new ArrayList<>();

  @Override
  public Annotation createErrorAnnotation(PsiElement elt, @Nullable String message) {
    errorElements.add(elt);
    errorMessages.add(message);
    Annotation stub = new Annotation(0, 0, HighlightSeverity.ERROR, "", "");
    createdAnnotations.add(stub);
    return stub;
  }

  @Override
  public Annotation createErrorAnnotation(ASTNode node, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createErrorAnnotation(TextRange range, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createWarningAnnotation(PsiElement elt, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createWarningAnnotation(ASTNode node, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createWarningAnnotation(TextRange range, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createWeakWarningAnnotation(PsiElement elt, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createWeakWarningAnnotation(ASTNode node, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createWeakWarningAnnotation(TextRange range, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createInfoAnnotation(PsiElement elt, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createInfoAnnotation(ASTNode node, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createInfoAnnotation(TextRange range, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createAnnotation(
      HighlightSeverity severity, TextRange range, @Nullable String message) {
    return null;
  }

  @Override
  public Annotation createAnnotation(
      HighlightSeverity severity,
      TextRange range,
      @Nullable String message,
      @Nullable String htmlTooltip) {
    return null;
  }

  @Override
  public AnnotationSession getCurrentAnnotationSession() {
    return null;
  }

  @Override
  public boolean isBatchMode() {
    return false;
  }
}
