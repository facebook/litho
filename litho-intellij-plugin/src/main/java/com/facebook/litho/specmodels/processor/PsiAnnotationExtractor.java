/*
 * Copyright 2017-present Facebook, Inc.
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
package com.facebook.litho.specmodels.processor;

import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.squareup.javapoet.AnnotationSpec;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;

/** Helper for extracting annotations from a given {@link TypeElement}. */
public class PsiAnnotationExtractor {

  public static ImmutableList<AnnotationSpec> extractValidAnnotations(
      Project project, PsiClass psiClass) {
    final List<AnnotationSpec> annotations = new ArrayList<>();

    for (PsiAnnotation annotation : psiClass.getModifierList().getAnnotations()) {
      if (isValidAnnotation(project, annotation)) {
        annotations.add(
            AnnotationSpec.builder(PsiTypeUtils.guessClassName(annotation.getQualifiedName()))
                .build());
      }
    }

    return ImmutableList.copyOf(annotations);
  }

  /**
   * We consider an annotation to be valid for extraction if it's not an internal annotation (i.e.
   * is in the <code>com.facebook.litho</code> package and is not a source-only annotation.
   *
   * @return Whether or not to extract the given annotation.
   */
  private static boolean isValidAnnotation(Project project, PsiAnnotation psiAnnotation) {
    final String text = psiAnnotation.getQualifiedName();
    PsiClass annotationClass = PsiSearchUtils.findClass(project, psiAnnotation.getQualifiedName());
    if (annotationClass == null) {
      throw new RuntimeException("Annotation class not found, text is: " + text);
    }

    final Retention retention =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(annotationClass, Retention.class);

    if (retention != null && retention.value() == RetentionPolicy.SOURCE) {
      return false;
    }

    return !text.startsWith("com.facebook.");
  }
}
