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

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.SpecElementType;
import java.util.function.Predicate;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class SpecElementTypeDeterminator {
  static boolean isKotlinSingleton(TypeElement element) {
    return element.getKind() == ElementKind.CLASS
        && element.getEnclosedElements().stream()
            .anyMatch(
                e -> {
                  final CharSequence instanceFieldName = "INSTANCE";
                  return e.getSimpleName().contentEquals(instanceFieldName)
                      && e.asType().toString().equals(element.getQualifiedName().toString())
                      && e.getModifiers()
                          .containsAll(
                              ImmutableList.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL));
                });
  }

  /**
   * Determine whether we are handling a Kotlin class.
   * For that to happen we check the AnnotationMirrors, in order to find an indication
   * that there is a `kotlin.Metadata` Annotation available.
   *
   * If one is found we also check that we are not a Kotlin singleton, since that
   * would require different handling.
   * */
  static boolean isKotlinClass(TypeElement element) {
    final boolean isClassKind = element.getKind() == ElementKind.CLASS;
    final boolean hasMetadataAnnotation = element
        .getAnnotationMirrors()
        .stream()
        .anyMatch(
            (Predicate<AnnotationMirror>) annotationMirror -> ((TypeElement) annotationMirror
                .getAnnotationType()
                .asElement()).getQualifiedName().toString().equals("kotlin.Metadata"));
    final boolean isKotlinSingleton = isKotlinSingleton(element);

    return isClassKind && hasMetadataAnnotation && !isKotlinSingleton;
  }

  public static SpecElementType determine(TypeElement element) {
    if (isKotlinSingleton(element)) {
      return SpecElementType.KOTLIN_SINGLETON;
    }

    if(isKotlinClass(element)) {
      return SpecElementType.KOTLIN_CLASS;
    }

    return SpecElementType.JAVA_CLASS;
  }
}
