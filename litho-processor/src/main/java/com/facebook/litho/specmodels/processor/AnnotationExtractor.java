/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

/** Helper for extracting annotations from a given {@link TypeElement}. */
public class AnnotationExtractor {
  public static ImmutableList<AnnotationSpec> extractValidAnnotations(TypeElement element) {
    final List<AnnotationSpec> annotations = new ArrayList<>();
    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      if (isValidAnnotation(annotationMirror)) {
        annotations.add(AnnotationSpec.get(annotationMirror));
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
  private static boolean isValidAnnotation(AnnotationMirror annotation) {
    try {
      final Retention retention =
          Class.forName(annotation.getAnnotationType().toString()).getAnnotation(Retention.class);

      if (retention != null && retention.value() == RetentionPolicy.SOURCE) {
        return false;
      }
    } catch (ClassNotFoundException e) {
      return false;
    }

    return !annotation.getAnnotationType().toString().startsWith("com.facebook.litho.");
  }
}
