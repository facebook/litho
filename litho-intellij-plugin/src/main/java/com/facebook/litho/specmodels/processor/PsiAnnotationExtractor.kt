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

package com.facebook.litho.specmodels.processor

import com.facebook.litho.intellij.PsiSearchUtils
import com.facebook.litho.specmodels.internal.ImmutableList
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.squareup.javapoet.AnnotationSpec
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/** Helper for extracting annotations from a given [javax.lang.model.element.TypeElement]. */
object PsiAnnotationExtractor {

  @JvmStatic
  fun extractValidAnnotations(project: Project, psiClass: PsiClass): ImmutableList<AnnotationSpec> {
    val annotations = ArrayList<AnnotationSpec>()

    val modifierList = psiClass.modifierList ?: return ImmutableList.of()
    for (annotation in modifierList.annotations) {
      if (isValidAnnotation(project, annotation)) {
        val qualifiedName = annotation.qualifiedName ?: continue
        annotations.add(AnnotationSpec.builder(PsiTypeUtils.guessClassName(qualifiedName)).build())
      }
    }

    return ImmutableList.copyOf(annotations)
  }

  @Suppress("UNCHECKED_CAST")
  private fun isValidAnnotation(project: Project, psiAnnotation: PsiAnnotation): Boolean {
    val text = psiAnnotation.qualifiedName ?: return false
    if (text.startsWith("com.facebook.")) {
      return false
    }
    val annotationClass = PsiSearchUtils.getInstance().findClass(project, text) ?: return false

    val retention: Retention? =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(
            annotationClass,
            Retention::class.java as Class<java.lang.annotation.Annotation>,
        ) as? Retention

    return retention == null || retention.value != RetentionPolicy.SOURCE
  }
}
