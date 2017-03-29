/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;

import java.util.List;
import java.util.Map;

/**
 * Utility class for processing specs.
 */
public class ProcessorUtils {

  /**
   * Gets an annotation parameter from an annotation. Usually you can just get the parameter
   * directly, but if the parameter has type {@link Class} it doesn't work, because javac doesn't
   * load classes in the normal manner.
   * @see https://area-51.blog/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
   * for more details.
   */
  public static <T> T getAnnotationParameter(
      Elements elements,
      Element element,
      Class<?> annotationType,
      String parameterName) {
    List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
    AnnotationMirror mirror = null;
    for (AnnotationMirror m : annotationMirrors) {
      if (m.getAnnotationType().toString().equals(annotationType.getCanonicalName())) {
        mirror = m;
        break;
      }
    }

    if (mirror == null) {
      return null;
    }

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        elements.getElementValuesWithDefaults(mirror).entrySet()) {
      if (parameterName.equals(entry.getKey().getSimpleName().toString())) {
        return (T) entry.getValue().getValue();
      }
    }

    return null;
  }
}
